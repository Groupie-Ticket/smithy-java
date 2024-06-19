package software.amazon.smithy.java.kestrel.codec;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import software.amazon.smithy.java.kestrel.Bufferer;
import software.amazon.smithy.java.runtime.core.serde.InitialMessageDecoder;

public final class BufferingKestrelInitialEventDecoder extends InitialMessageDecoder<ByteBuffer> {
    private final Bufferer bufferer;

    public BufferingKestrelInitialEventDecoder(CompletableFuture<ByteBuffer> callback) {
        super(callback);
        this.bufferer = new Bufferer(b -> queue.add(ByteBuffer.wrap(b)));
    }

    @Override
    protected void feed(ByteBuffer byteBuffer) throws Throwable {
        bufferer.feed(byteBuffer);
    }
}
