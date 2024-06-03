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
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.netty.util.ReferenceCountUtil;
import software.amazon.smithy.java.server.core.*;

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
        }
        return request;
    }

    private static void writeResponse(Channel channel, CompletableFuture<Job> future) {
        future.whenComplete((job, throwable) -> {
            ByteValue byteValue = job.getReply().getValue();
            ByteBuf content = Unpooled.wrappedBuffer(byteValue.value());
            HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
            response.headers().set("x-amzn-RequestId", job.getRequest().getRequestId());
            channel.writeAndFlush(response);
        });
    }

}
