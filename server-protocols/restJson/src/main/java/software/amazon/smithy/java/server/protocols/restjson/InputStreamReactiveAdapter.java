package software.amazon.smithy.java.server.protocols.restjson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class InputStreamReactiveAdapter implements Flow.Publisher<ByteBuffer> {

    private final InputStream inputStream;
    private final AtomicLong pendingReads = new AtomicLong(0);
    private final AtomicReference<Flow.Subscriber<? super ByteBuffer>> subscriber = new AtomicReference<>();

    public InputStreamReactiveAdapter(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        if (!this.subscriber.compareAndSet(null, subscriber)) {
            throw new IllegalArgumentException("Can only subscribe once");
        }
        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
                if (n <= 0) {
                    throw new IllegalArgumentException("requested " + n + " items");
                }

                var oldReads = pendingReads.getAndUpdate(old -> {
                    try {
                        return Math.addExact(old, n);
                    } catch (ArithmeticException ignore) {
                        return Long.MAX_VALUE;
                    }
                });

                if (oldReads == 0) {
                    ForkJoinPool.commonPool().submit(() -> readNext());
                }
            }

            @Override
            public void cancel() {

            }
        });
    }

    private void readNext() {
        try {
            byte[] buffer = new byte[16384];
            int read = inputStream.read(buffer);
            subscriber.get().onNext(ByteBuffer.wrap(buffer, 0, read));
            if (pendingReads.decrementAndGet() > 0) {
                ForkJoinPool.commonPool().submit(this::readNext);
            }
        } catch (IOException e) {
            subscriber.get().onError(e);
        }
    }
}
