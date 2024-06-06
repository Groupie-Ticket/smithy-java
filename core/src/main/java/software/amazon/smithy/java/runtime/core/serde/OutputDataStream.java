/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core.serde;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

final class OutputDataStream implements DataStream {

    private final Consumer<OutputStream> outputStreamConsumer;
    private final Executor executor;
    private final BlockingBufferingOutputStream bos = new BlockingBufferingOutputStream();
    private final AtomicLong pendingReads = new AtomicLong(0);
    private final AtomicReference<Flow.Subscriber<? super ByteBuffer>> subscriber = new AtomicReference<>();

    OutputDataStream(Consumer<OutputStream> outputStreamConsumer) {
        this(outputStreamConsumer, ForkJoinPool.commonPool());
    }

    OutputDataStream(Consumer<OutputStream> outputStreamConsumer, Executor executor) {
        this.outputStreamConsumer = outputStreamConsumer;
        this.executor = executor;
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
                    executor.execute(() -> startRead());
                }

                if (oldReads == 0) {
                    executor.execute(() -> readNext());
                }
            }

            @Override
            public void cancel() {
                try {
                    bos.close();
                } catch (IOException e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    private void startRead() {
        try {
            outputStreamConsumer.accept(bos);
        } catch (Throwable t) {
            Flow.Subscriber<? super ByteBuffer> s = subscriber.get();
            if (s != null) {
                s.onError(t);
            }
        }
    }

    private void readNext() {
        ByteBuffer bb;
        try {
            bb = bos.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            subscriber.get().onError(e);
            return;
        }

        if (bb == null) {
            subscriber.get().onComplete();
        } else {
            subscriber.get().onNext(bb);
            if (pendingReads.decrementAndGet() > 0) {
                executor.execute(this::readNext);
            }
        }
    }

    @Override
    public long contentLength() {
        return -1;
    }

    @Override
    public Optional<String> contentType() {
        return Optional.empty();
    }

    private static final class BlockingBufferingOutputStream extends OutputStream {
        private static final ByteBuffer CLOSE_SENTINEL = ByteBuffer.wrap(new byte[0]);

        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final ArrayBlockingQueue<ByteBuffer> queue = new ArrayBlockingQueue<>(4);
        private ByteBuffer currentBuffer = ByteBuffer.allocate(4096);

        @Override
        public void write(int b) throws IOException {
            if (closed.get()) {
                throw new IOException("stream closed");
            }
            currentBuffer.put((byte) b);
            rotateIfNeeded();
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (closed.get()) {
                throw new IOException("stream closed");
            }

            int writeLen = Math.min(len, currentBuffer.remaining());
            currentBuffer.put(b, off, writeLen);
            rotateIfNeeded();
            if (writeLen < len) {
                write(b, off + writeLen, len - writeLen);
            }
        }

        private void rotateIfNeeded() throws IOException {
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
