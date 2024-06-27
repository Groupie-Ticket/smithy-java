/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.netty;

import static java.lang.String.format;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

final class RequestBodyPublisher implements Flow.Publisher<ByteBuffer> {
    //Guards the requestBodySubscriber;
    private final Object requestBodySubscriberLock = new Object();
    private final Channel channel;
    private final Executor downstreamExecutor;
    private final Duration maxSwallowDuration;
    private final Supplier<Instant> clock = Instant::now;
    private final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
    private final AtomicInteger pendingWrites = new AtomicInteger();
    private long pendingReads = 0;
    private Instant swallowStart;
    private Flow.Subscriber<? super ByteBuffer> requestBodySubscriber;
    private boolean closed;
    private boolean readRequested = false;
    private Throwable closedException;

    private record Complete(ByteBuffer buf) {}

    RequestBodyPublisher(Channel channel, Executor downstreamExecutor) {
        this.channel = channel;
        this.downstreamExecutor = downstreamExecutor;
        this.maxSwallowDuration = Duration.ofSeconds(10);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> s) {
        synchronized (requestBodySubscriberLock) {
            if (requestBodySubscriber != null) {
                throw new IllegalStateException(
                    format(
                        "Unable to subscribe; a subscriber, %s, already exists",
                        requestBodySubscriber
                    )
                );
            }
            requestBodySubscriber = s;
            s.onSubscribe(new Flow.Subscription() {
                private boolean firstRequest = true;

                @Override
                public void request(long n) {
                    synchronized (requestBodySubscriberLock) {
                        if (n <= 0) {
                            error(new IllegalArgumentException("Requested " + n + " items"));
                            return;
                        }

                        if (firstRequest) {
                            firstRequest = false;
                            if (closedException != null) {
                                // there is no need to wrap an IOException or SocketException
                                // the layer that catches this exception generally expects those types
                                if (closedException instanceof IOException) {
                                    s.onError(closedException);
                                } else {
                                    s.onError(
                                        new IOException(
                                            "The request body has already " +
                                                "terminated erroneously",
                                            closedException
                                        )
                                    );
                                }
                                return;
                            } else if (closed) {
                                s.onError(new IOException("The request body has already finished streaming"));
                                return;
                            }

                            if (pendingWrites.get() > 0) {
                                downstreamExecutor.execute(() -> flushWrites(requestBodySubscriber));
                            }
                        }

                        if (closed) {
                            return;
                        }

                        // We increment the number of pending reads while avoiding overflow, which could result in
                        // deadlock.
                        try {
                            pendingReads = Math.addExact(pendingReads, n);
                        } catch (ArithmeticException ignore) {
                            pendingReads = Long.MAX_VALUE;
                        }

                        if (!readRequested) {
                            readRequested = true;
                            pendingReads--;
                            channel.eventLoop().submit(channel::read);
                        }
                    }
                }

                @Override
                public void cancel() {
                    synchronized (requestBodySubscriberLock) {
                        requestBodySubscriber = null;
                        swallowRemainingRequestData();
                    }
                }
            });
        }
    }

    void next(ByteBuf buf) {
        synchronized (requestBodySubscriberLock) {
            readRequested = false;
            byte[] copy = new byte[buf.readableBytes()];
            buf.readBytes(copy);
            buf.release();
            enqueue(ByteBuffer.wrap(copy), requestBodySubscriber);
            if (swallowStart != null && !maxSwallowDuration.isNegative() && !maxSwallowDuration.isZero() &&
                clock.get().isAfter(swallowStart.plus(maxSwallowDuration))) {
                pendingReads = 0;
                channel.eventLoop().submit(() -> channel.close());
            } else if (pendingReads > 0 && !readRequested) {
                readRequested = true;
                pendingReads--;
                channel.eventLoop().submit(channel::read);
            }
        }
    }

    private void flushWrites(Flow.Subscriber<? super ByteBuffer> sub) {
        int toWrite = pendingWrites.get();
        while (toWrite > 0) {
            int written = 0;
            try {
                for (int i = 0; i < toWrite; i++) {
                    var event = queue.poll();
                    if (event == null) {
                        return;
                    }

                    written--;
                    if (event instanceof ByteBuffer buf) {
                        sub.onNext(buf);
                    } else if (event instanceof Throwable t) {
                        sub.onError(t);
                        queue.clear();
                        pendingWrites.set(0);
                        written = 0;
                    } else {
                        ByteBuffer buf = ((Complete) event).buf;
                        if (buf.hasRemaining()) {
                            sub.onNext(buf);
                        }
                        sub.onComplete();
                        closed = true;
                    }
                }
            } finally {
                if (written != 0) {
                    toWrite = pendingWrites.addAndGet(written);
                }
            }
        }
    }

    private void enqueue(Object o, Flow.Subscriber<? super ByteBuffer> requestBodySubscriber) {
        queue.add(o);
        if (pendingWrites.getAndIncrement() == 0 && requestBodySubscriber != null) {
            // sub is only safe to access under the lock, so pass it in
            downstreamExecutor.execute(() -> flushWrites(requestBodySubscriber));
        }
    }

    void complete(ByteBuf buf) {
        synchronized (requestBodySubscriberLock) {
            byte[] copy = new byte[buf.readableBytes()];
            buf.readBytes(copy);
            buf.release();
            enqueue(new Complete(ByteBuffer.wrap(copy)), requestBodySubscriber);
        }
    }

    /**
     * Signal that an error was encountered while streaming the request.
     */
    void error(Throwable throwable) {
        synchronized (requestBodySubscriberLock) {
            closed = true;
            closedException = throwable;
            if (requestBodySubscriber != null) {
                var sub = requestBodySubscriber;
                enqueue(throwable, sub);
                requestBodySubscriber = null;
            }
            swallowRemainingRequestData();
        }
    }

    private void swallowRemainingRequestData() {
        swallowStart = clock.get();
        pendingReads = Long.MAX_VALUE;
        channel.read();
    }
}
