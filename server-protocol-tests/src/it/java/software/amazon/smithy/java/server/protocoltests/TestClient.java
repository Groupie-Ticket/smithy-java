/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocoltests;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpVersion;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

final class TestClient {

    private final URI endpoint;
    private final Bootstrap clientBootstrap;

    public TestClient(URI endpoint) {
        this.endpoint = endpoint;
        clientBootstrap = new Bootstrap()
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("codec", new HttpClientCodec());
                    pipeline.addLast("aggregator", new HttpObjectAggregator(Integer.MAX_VALUE));
                }
            })
            .group(new NioEventLoopGroup())
            .channelFactory(NioSocketChannel::new);
    }

    FullHttpResponse sendRequest(EndToEndProtocolTests.HttpRequest request) {
        var path = endpoint.getPath();
        if (request.uri() != null && !request.uri().isEmpty()) {
            if (path.endsWith("/") && request.uri().startsWith("/")) {
                path = path + request.uri().substring(1);
            } else {
                path = path + request.uri();
            }
        }
        String queryParams;
        if (request.queryParams() == null || request.queryParams().isEmpty()) {
            queryParams = null;
        } else {
            queryParams = "?" + request.queryParams().stream().collect(Collectors.joining("&"));
        }
        ByteBuf body = request.body()
            .map(b -> Unpooled.wrappedBuffer(b.getBytes(StandardCharsets.UTF_8)))
            .orElse(Unpooled.EMPTY_BUFFER);
        try {
            var req = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.valueOf(request.method()),
                new URI(
                    endpoint.getScheme(),
                    endpoint.getUserInfo(),
                    endpoint.getHost(),
                    endpoint.getPort(),
                    path,
                    queryParams,
                    null
                ).toString(),
                body
            );
            if (body.readableBytes() > 0) {
                req.headers().add(HttpHeaderNames.CONTENT_LENGTH, body.readableBytes());
                request.bodyMediaType().ifPresent(mt -> {
                    req.headers().add(HttpHeaderNames.CONTENT_TYPE, mt);
                });
            }
            return call(req);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    FullHttpResponse call(FullHttpRequest request) {
        var channel = connect();
        var cf = new CompletableFuture<FullHttpResponse>();
        channel.pipeline().addLast("resp", new SimpleChannelInboundHandler<FullHttpResponse>() {

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
                cf.complete(msg.duplicate());
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                cf.completeExceptionally(cause);
            }
        });
        channel.writeAndFlush(request);
        try {
            // for now, close the connection eagerly; later, we can reuse
            return cf.whenComplete((res, ex) -> {
                channel.pipeline().remove("resp");
                channel.close();
            }).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e.getCause());
        }
    }

    Channel connect() {
        RuntimeException e = null;

        for (int i = 0; i < 3; i++) {
            ChannelFuture connectFuture = clientBootstrap.connect(
                new InetSocketAddress(endpoint.getHost(), endpoint.getPort())
            );
            var clientChannel = connectFuture.awaitUninterruptibly().channel();

            if (connectFuture.isSuccess()) {
                return clientChannel;
            }

            e = new RuntimeException(
                "Unable to connect to server : "
                    + connectFuture.cause()
            );
        }
        throw e;
    }

    void shutdown() {
        // right now, with no connection reuse, nothing
    }
}
