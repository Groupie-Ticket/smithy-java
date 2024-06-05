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
import java.net.http.HttpHeaders;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.netty.handler.codec.http.HttpHeaders.Names;
import software.amazon.smithy.java.server.core.*;
import software.amazon.smithy.java.server.core.attributes.HttpAttributes;

@ChannelHandler.Sharable
final class NettyHandler extends ChannelDuplexHandler {

    private final Orchestrator orchestrator;

    NettyHandler(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest httpRequest) {
            Channel channel = ctx.channel();
            Request request = createRequest(httpRequest, channel);
            Job job = new JobImpl(request, new ReplyImpl());
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
            setHeaders(request, fullHttpRequest);
        }
        return request;
    }

    //TODO Fix this after we decide on a header implementation
    private void setHeaders(Request request, FullHttpRequest fullHttpRequest) {
        Map<String, List<String>> headers = new HashMap<>();
        for (String header : fullHttpRequest.headers().names()) {
            headers.put(header, fullHttpRequest.headers().getAll(header));
        }
        request.getContext().put(HttpAttributes.HTTP_HEADERS, HttpHeaders.of(headers, (k, v) -> true));
    }

    //TODO Fix this after we decide on a header implementation
    private static void setHeaders(Reply reply, HttpResponse response) {
        reply.getContext().get(HttpAttributes.HTTP_HEADERS).map().forEach(response.headers()::set);
    }

    private static void writeResponse(Channel channel, CompletableFuture<Job> future) {
        future.whenComplete((job, throwable) -> {
            try {
                ByteValue byteValue = job.getReply().getValue();
                ByteBuf content = Unpooled.wrappedBuffer(byteValue.value());
                HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
                setHeaders(job.getReply(), response);
                response.headers().set(Names.CONTENT_LENGTH, content.readableBytes());
                channel.writeAndFlush(response);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
