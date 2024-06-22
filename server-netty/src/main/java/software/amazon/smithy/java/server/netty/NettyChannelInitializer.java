/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.netty;

import static io.netty.handler.codec.http2.Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME;
import static io.netty.util.AsciiString.contentEquals;

import io.netty.channel.*;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.*;
import io.netty.handler.logging.LogLevel;
import io.netty.util.ReferenceCountUtil;
import java.util.List;
import java.util.function.Consumer;


final class NettyChannelInitializer extends ChannelInitializer<Channel> {

    private static final System.Logger WIRE = System.getLogger("WIRE");
    private static final boolean WIRE_ENABLED = WIRE.isLoggable(System.Logger.Level.INFO);

    private final Consumer<ChannelPipeline> handlerInstaller;

    NettyChannelInitializer(Consumer<ChannelPipeline> handlerInstaller) {
        this.handlerInstaller = handlerInstaller;

    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();

        Http2FrameCodecBuilder codecBuilder = Http2FrameCodecBuilder.forServer();
        if (WIRE_ENABLED) {
            codecBuilder.frameLogger(new Http2FrameLogger(LogLevel.INFO, "WIRE"));
        }
        pipeline.addLast(getUpgradeHandler(new HttpServerCodec(), codecBuilder.build()));
        installUpgradeListener(pipeline, new Http2MultiplexHandler(new H2ChannelInitializer(handlerInstaller)));
        handlerInstaller.accept(pipeline);
        pipeline.read();

    }

    private static final class H2ChannelInitializer extends ChannelInitializer<Channel> {

        private final Consumer<ChannelPipeline> handlerInstaller;

        private H2ChannelInitializer(Consumer<ChannelPipeline> handlerInstaller) {
            this.handlerInstaller = handlerInstaller;
        }

        @Override
        protected void initChannel(Channel channel) throws Exception {
            channel.pipeline().addLast("http2Codec", new Http2StreamFrameToHttpObjectCodec(true));
            handlerInstaller.accept(channel.pipeline());
        }
    }

    private static ChannelHandler getUpgradeHandler(HttpServerCodec httpServerCodec, Http2FrameCodec http2Codec) {
        HttpServerUpgradeHandler.UpgradeCodecFactory upgradeCodecFactory = protocol -> contentEquals(
            HTTP_UPGRADE_PROTOCOL_NAME,
            protocol
        ) ? new Http2ServerUpgradeCodec("http2Codec", http2Codec) : null;
        HttpServerUpgradeHandler upgradeHandler = new RetainingHttpServerUpgradeHandler(
            httpServerCodec,
            upgradeCodecFactory
        );
        return new CleartextHttp2ServerUpgradeHandler(httpServerCodec, upgradeHandler, http2Codec);
    }

    private static void installUpgradeListener(ChannelPipeline pipeline, Http2MultiplexHandler http2Handler) {
        // This handler listens for HTTP/2 upgrade events, resulting from either HTTP Upgrade or Prior Knowledge.
        // When an upgrade occurs, the original channel becomes multiplexed (meaning that requests get routed to
        // child channels), so we remove the installed handlers from the channel.
        pipeline.addLast("upgradeListener", new ChannelInboundHandlerAdapter() {
            @Override
            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                if (evt instanceof HttpServerUpgradeHandler.UpgradeEvent
                    || evt instanceof CleartextHttp2ServerUpgradeHandler.PriorKnowledgeUpgradeEvent) {
                    // Remove this handler and everything after it
                    while (pipeline.removeLast() != this) {
                        // do nothing
                    }
                    pipeline.addLast("http2Handler", http2Handler);
                    pipeline.addLast("http2FrameReleaser", new H2FrameReleaser());
                    ctx.channel().read();
                } else {
                    super.userEventTriggered(ctx, evt);
                }
            }
        });
    }

    private static final class H2FrameReleaser extends ChannelDuplexHandler {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof Http2Frame) {
                ReferenceCountUtil.release(msg);
            } else {
                ctx.fireChannelRead(msg);
            }
        }
    }

    private static final class RetainingHttpServerUpgradeHandler extends HttpServerUpgradeHandler {
        RetainingHttpServerUpgradeHandler(
            HttpServerCodec httpServerCodec,
            UpgradeCodecFactory upgradeCodecFactory
        ) {
            super(httpServerCodec, upgradeCodecFactory);
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out) {
            // no-op, remove this if/when we want to support h2c http upgrades
            ReferenceCountUtil.retain(msg);
            out.add(msg);
        }
    }
}
