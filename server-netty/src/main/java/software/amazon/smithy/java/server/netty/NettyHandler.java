/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpHeaders.Names;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import software.amazon.smithy.java.server.core.*;
import software.amazon.smithy.java.server.core.attributes.HttpAttributes;

@ChannelHandler.Sharable
final class NettyHandler extends ChannelDuplexHandler {

    private final Orchestrator orchestrator;
    private final ProtocolResolver protocolResolver;

    NettyHandler(Orchestrator orchestrator, ProtocolResolver protocolResolver) {
        this.orchestrator = orchestrator;
        this.protocolResolver = protocolResolver;
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest httpRequest) {
            Channel channel = ctx.channel();
            URI uri = URI.create(httpRequest.uri());
            HttpHeaders headers = getHeaders(httpRequest);
            var operationProtocolPair = protocolResolver.resolveOperation(
                ResolutionRequest
                    .builder()
                    .uri(uri)
                    .headers(headers)
                    .build()
            );
            Request request = createRequest(httpRequest, channel);
            request.getContext().put(HttpAttributes.HTTP_HEADERS, headers);
            request.getContext().put(HttpAttributes.HTTP_URI, uri);
            Job job = new JobImpl(request, new ReplyImpl(), operationProtocolPair.left, operationProtocolPair.right);
            writeResponse(channel, orchestrator.enqueue(job));
        }
    }

    private Request createRequest(HttpRequest httpRequest, Channel channel) {
        String requestId = UUID.randomUUID().toString();
        Request request = new RequestImpl(requestId);
        if (httpRequest instanceof FullHttpRequest fullHttpRequest) {
            ByteBuf content = fullHttpRequest.content();
            byte[] buffer = new byte[content.readableBytes()];
            content.readBytes(buffer);
            content.release();
            request.setValue(new ByteValue(buffer));
        }
        return request;
    }

    //TODO Fix this after we decide on a header implementation
    private HttpHeaders getHeaders(FullHttpRequest fullHttpRequest) {
        Map<String, List<String>> headers = new HashMap<>();
        for (String header : fullHttpRequest.headers().names()) {
            headers.put(header, fullHttpRequest.headers().getAll(header));
        }
        return HttpHeaders.of(headers, (k, v) -> true);
    }

    //TODO Fix this after we decide on a header implementation
    private static void setHeaders(Reply reply, HttpResponse response) {
        for (var entry : reply.context().get(HttpAttributes.HTTP_HEADERS).map().entrySet()) {
            response.headers().add(entry.getKey(), entry.getValue());
        }
    }

    private static void writeResponse(Channel channel, CompletableFuture<Job> future) {
        future.whenComplete((job, throwable) -> {
            if (throwable == null) {
                try {
                    if (job.getFailure().isPresent()) {
                        sendErrorResponse(job.getFailure().get(), channel);
                        return;
                    }
                    ByteValue byteValue = job.reply().getValue();
                    ByteBuf content = Unpooled.wrappedBuffer(byteValue.get());
                    HttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        content
                    );
                    setHeaders(job.reply(), response);
                    response.headers().add(Names.CONTENT_LENGTH, content.readableBytes());
                    channel.writeAndFlush(response);
                } catch (Exception e) {
                    job.setFailure(e);
                    sendErrorResponse(e, channel);
                }
            } else {
                sendErrorResponse(throwable, channel);
            }
        });
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
    }

}
