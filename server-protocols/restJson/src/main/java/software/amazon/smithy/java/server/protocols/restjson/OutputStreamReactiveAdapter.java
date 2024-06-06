package software.amazon.smithy.java.server.protocols.restjson;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class OutputStreamReactiveAdapter implements Flow.Publisher<ByteBuffer> {
    private static final ExecutorService EXEC = Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("OutputStreamReader-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    });

    private final Consumer<OutputStream> outputStreamConsumer;
    private final BlockingBufferingOutputStream bos = new BlockingBufferingOutputStream();
    private final AtomicLong pendingReads = new AtomicLong(0);
    private final AtomicReference<Flow.Subscriber<? super ByteBuffer>> subscriber = new AtomicReference<>();

    public OutputStreamReactiveAdapter(Consumer<OutputStream> outputStreamConsumer) {
        this.outputStreamConsumer = outputStreamConsumer;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        if (!this.subscriber.compareAndSet(null, subscriber)) {
            throw new IllegalArgumentException("Can only subscribe once");
        }
        subscriber.onSubscribe(new Flow.Subscription() {
            private final AtomicBoolean firstRequest = new AtomicBoolean(false);

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

                if (firstRequest.compareAndSet(false, true)) {
                    EXEC.submit(() -> startRead());
                }

                if (oldReads == 0) {
                    EXEC.submit(() -> readNext());
                }
            }

            @Override
            public void cancel() {

            }
        });
    }

    private void startRead() {
        outputStreamConsumer.accept(bos);
    }

    private void readNext() {
        ByteBuffer bb;
        try {
            bb = bos.take();
        } catch (InterruptedException e) {
            subscriber.get().onError(e);
            return;
        }

        if (bb == null) {
            subscriber.get().onComplete();
        } else {
            subscriber.get().onNext(bb);
            if (pendingReads.decrementAndGet() > 0) {
                EXEC.submit(this::readNext);
            }
        }
    }

    private static final class BlockingBufferingOutputStream extends OutputStream {
        private static final ByteBuffer CLOSE_SENTINEL = ByteBuffer.wrap(new byte[0]);

        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final ArrayBlockingQueue<ByteBuffer> queue = new ArrayBlockingQueue<>(4);
        private ByteBuffer currentBuffer = ByteBuffer.allocate(4096);

        @Override
        public void write(int b) throws IOException {
            currentBuffer.put((byte) b);
            if (!currentBuffer.hasRemaining()) {
                try {
                    queue.put(currentBuffer.flip());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted waiting to write");
                }
                currentBuffer = ByteBuffer.allocate(4096);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            int initialLen = Math.min(len, currentBuffer.remaining());
            currentBuffer.put(b, off, initialLen);
            if (!currentBuffer.hasRemaining()) {
                try {
                    queue.put(currentBuffer.flip());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted waiting to write");
                }
                currentBuffer = ByteBuffer.allocate(4096);
            }
            if (initialLen < len) {
                write(b, off + initialLen, len - initialLen);
            }
        }

        ByteBuffer take() throws InterruptedException {
            ByteBuffer retVal = queue.take();
            if (retVal == CLOSE_SENTINEL) {
                return null;
            }
            return retVal;
        }

        @Override
        public void close() throws IOException {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            try {
                queue.put(currentBuffer.flip());
                queue.put(CLOSE_SENTINEL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while closing");
            }
            currentBuffer = null;
        }
    }
}
