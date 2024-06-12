/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.smithy.java.runtime.core.schema.ApiOperation;
import software.amazon.smithy.java.server.core.ByteValue;
import software.amazon.smithy.java.server.core.Job;
import software.amazon.smithy.java.server.core.JobImpl;
import software.amazon.smithy.java.server.core.Orchestrator;
import software.amazon.smithy.java.server.core.ProtocolResolver;
import software.amazon.smithy.java.server.core.ReactiveByteValue;
import software.amazon.smithy.java.server.core.Reply;
import software.amazon.smithy.java.server.core.ReplyImpl;
import software.amazon.smithy.java.server.core.Request;
import software.amazon.smithy.java.server.core.RequestImpl;
import software.amazon.smithy.java.server.core.ResolutionRequest;
import software.amazon.smithy.java.server.core.Value;
import software.amazon.smithy.java.server.core.attributes.HttpAttributes;

final class NettyHandler extends ChannelDuplexHandler {

    private final Orchestrator orchestrator;
    private final ProtocolResolver protocolResolver;
    private ApiOperation<?, ?> operation;
    private Job job;
    private RequestBodyPublisher bodyPublisher;
    private ByteArrayOutputStream bodyAccumulator;

    NettyHandler(Orchestrator orchestrator, ProtocolResolver protocolResolver) {
        this.orchestrator = orchestrator;
        this.protocolResolver = protocolResolver;
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel channel = ctx.channel();
        if (msg instanceof HttpRequest httpRequest) {
            URI uri = URI.create(httpRequest.uri());
            HttpHeaders headers = getHeaders(httpRequest);
            var operationProtocolPair = protocolResolver.resolveOperation(
                ResolutionRequest
                    .builder()
                    .uri(uri)
                    .headers(headers)
                    .build()
            );
            operation = operationProtocolPair.left.getApiOperation();
            Request request = createRequest(httpRequest);
            request.getContext().put(HttpAttributes.HTTP_HEADERS, headers);
            request.getContext().put(HttpAttributes.HTTP_URI, uri);

            job = new JobImpl(request, new ReplyImpl(), operationProtocolPair.left, operationProtocolPair.right);

            if (httpRequest instanceof FullHttpRequest) {
                writeResponse(channel, orchestrator.enqueue(job));
                reset();
            }

            // If we have an HttpRequest but not an FullHttpRequest
            // we need to read a series of 0 or more HttpContents followed by an HttpLastContent

            if (operationProtocolPair.left.getApiOperation().streamingInput()) {
                channel.config().setAutoRead(false);
                bodyPublisher = new RequestBodyPublisher(channel);
                request.setValue(new ReactiveByteValue(bodyPublisher));
                writeResponse(channel, orchestrator.enqueue(job));
            } else {
                // TODO: this can be much more efficient
                bodyAccumulator = new ByteArrayOutputStream();
            }
        } else if (msg instanceof HttpContent content) {
            boolean last = msg instanceof LastHttpContent;
            if (bodyPublisher == null && bodyAccumulator == null) {
                content.release();
                throw new IllegalStateException("Content came in independent of the first message");
            }
            if (bodyPublisher != null) {
                if (last) {
                    bodyPublisher.complete(content.content());
                } else {
                    bodyPublisher.next(content.content());
                }
            } else {
                content.content().forEachByte(b -> {
                    bodyAccumulator.write(b);
                    return true;
                });
                content.release();
            }

            if (last) {
                if (!operation.streamingInput()) {
                    byte[] fullContent = bodyAccumulator.toByteArray();
                    job.request().setValue(new ByteValue(fullContent));
                    writeResponse(channel, orchestrator.enqueue(job));
                }

                reset();
                channel.config().setAutoRead(true);
            }
        }
    }

    private Request createRequest(HttpRequest httpRequest) {
        String requestId = UUID.randomUUID().toString();
        Request request = new RequestImpl(requestId);
        if (httpRequest instanceof FullHttpRequest fullHttpRequest) {
            if (operation.streamingInput()) {
                request.setValue(getSingleShotPublisher(fullHttpRequest.content()));
            } else {
                ByteBuf content = fullHttpRequest.content();
                byte[] buffer = new byte[content.readableBytes()];
                content.readBytes(buffer);
                content.release();
                request.setValue(new ByteValue(buffer));
            }
        }
        return request;
    }

    private Value<?> getSingleShotPublisher(ByteBuf content) {
        return new ReactiveByteValue(
            subscriber -> subscriber.onSubscribe(new SingleShotSubscription(content, subscriber))
        );
    }

    //TODO Fix this after we decide on a header implementation
    private HttpHeaders getHeaders(HttpMessage httpMessage) {
        Map<String, List<String>> headers = new HashMap<>();
        for (String header : httpMessage.headers().names()) {
            headers.put(header, httpMessage.headers().getAll(header));
        }
        return HttpHeaders.of(headers, (k, v) -> true);
    }

    //TODO Fix this after we decide on a header implementation
    private static void setHeaders(Reply reply, HttpResponse response) {
        boolean hasContentLength = false;
        boolean hasTransferEncoding = false;
        for (var entry : reply.context().get(HttpAttributes.HTTP_HEADERS).map().entrySet()) {
            response.headers().add(entry.getKey(), entry.getValue());
            hasContentLength |= entry.getKey().equalsIgnoreCase(HttpHeaderNames.CONTENT_LENGTH.toString());
            hasTransferEncoding |= entry.getKey().equalsIgnoreCase(HttpHeaderNames.TRANSFER_ENCODING.toString());
        }
        if (reply.getValue() instanceof ReactiveByteValue && !hasContentLength) {
            response.headers().add(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
        }
    }

    private static void writeResponse(Channel channel, CompletableFuture<Job> future) {
        future.whenCompleteAsync((job, throwable) -> {
            if (throwable == null) {
                try {
                    if (job.getFailure().isPresent()) {
                        sendErrorResponse(job.getFailure().get(), channel);
                        return;
                    }
                    HttpResponse response;
                    if (job.reply().getValue() instanceof ByteValue byteValue) {
                        ByteBuf content = Unpooled.wrappedBuffer(byteValue.get());
                        response = new DefaultFullHttpResponse(
                            HttpVersion.HTTP_1_1,
                            HttpResponseStatus.OK,
                            content
                        );
                        response.headers().add(Names.CONTENT_LENGTH, content.readableBytes());
                    } else {
                        response = new DefaultHttpResponse(
                            HttpVersion.HTTP_1_1,
                            HttpResponseStatus.OK
                        );
                    }
                    setHeaders(job.reply(), response);
                    channel.writeAndFlush(response).addListener(f -> {
                        if (f.isSuccess() && job.reply().getValue() instanceof ReactiveByteValue reactiveByteValue) {
                            AtomicReference<Flow.Subscription> sub = new AtomicReference<>();
                            reactiveByteValue.get().subscribe(new Flow.Subscriber<>() {
                                @Override
                                public void onSubscribe(Flow.Subscription s) {
                                    sub.set(s);
                                    s.request(1);
                                }

                                @Override
                                public void onNext(ByteBuffer item) {
                                    channel.writeAndFlush(new DefaultHttpContent(Unpooled.wrappedBuffer(item)))
                                        .addListener(f -> {
                                            if (f.isSuccess()) {
                                                sub.get().request(1);
                                            }
                                        });
                                }

                                @Override
                                public void onError(Throwable throwable) {
                                    channel.close();
                                }

                                @Override
                                public void onComplete() {
                                    channel.writeAndFlush(new DefaultLastHttpContent());
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    job.setFailure(e);
                    sendErrorResponse(e, channel);
                }
            } else {
                sendErrorResponse(throwable, channel);
            }
        }, channel.eventLoop());
    }

    private void reset() {
        operation = null;
        job = null;
        bodyPublisher = null;
        bodyAccumulator = null;
    }

    private static void sendErrorResponse(Throwable throwable, Channel channel) {
        throwable.printStackTrace();
        ByteBuf content = Unpooled.wrappedBuffer(throwable.getClass().getSimpleName().getBytes(StandardCharsets.UTF_8));
        HttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.INTERNAL_SERVER_ERROR,
            content
        );
        response.headers().add(Names.CONTENT_LENGTH, content.readableBytes());
        channel.writeAndFlush(response);

        //TODO: figure out when it is safe to not close here
        channel.close();
    }

    private static class SingleShotSubscription implements Flow.Subscription {
        private final ByteBuf content;
        private final Flow.Subscriber<? super ByteBuffer> subscriber;
        private boolean sent;

        public SingleShotSubscription(ByteBuf content, Flow.Subscriber<? super ByteBuffer> subscriber) {
            this.content = content;
            this.subscriber = subscriber;
            sent = false;
        }

        @Override
        public void request(long n) {
            if (sent) {
                return;
            }
            subscriber.onNext(content.copy().nioBuffer());
            content.release();
            subscriber.onComplete();
            sent = true;
        }

        @Override
        public void cancel() {
            if (!sent) {
                content.release();
                sent = true;
            }
        }
    }
}
