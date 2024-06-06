package software.amazon.smithy.java.server.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class RequestBodyPublisher implements Flow.Publisher<ByteBuffer> {
    private final Channel channel;
    private final Duration maxSwallow = Duration.ofSeconds(5);
    private Supplier<Instant> clock = Instant::now;
    private long pendingReads = 0;
    private Instant swallowStart;
    private final AtomicReference<Flow.Subscriber<? super ByteBuffer>> subscriber = new AtomicReference<>();
    private boolean closed;
    private boolean readRequested = false;
    private Throwable closedException;

    public RequestBodyPublisher(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        if (!this.subscriber.compareAndSet(null, subscriber)) {
            throw new IllegalArgumentException("Can only subscribe once");
        }

        channel.eventLoop().submit(() -> subscriber.onSubscribe(new Flow.Subscription() {
            private boolean firstRequest = true;

            @Override
            public void request(long n) {
                if (n <= 0) {
                    error(new IllegalArgumentException("Requested " + n + " items."));
                    return;
                }

                if (firstRequest) {
                    firstRequest = false;
                    if (closedException != null) {
                        // there is no need to wrap an IOException or SocketException
                        // the layer that catches this exception generally expects those types
                        if (closedException instanceof IOException) {
                            subscriber.onError(closedException);
                        } else {
                            subscriber.onError(
                                new IOException(
                                    "The request body has already " +
                                        "terminated erroneously",
                                    closedException
                                )
                            );
                        }
                        return;
                    } else if (closed) {
                        subscriber.onError(new IOException("The request body has already finished streaming"));
                        return;
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

            @Override
            public void cancel() {

            }
        }));
    }

    void next(ByteBuf buf) {
        if (!channel.eventLoop().inEventLoop()) {
            throw new IllegalStateException("next() should only come from the event loop");
        }
        var sub = subscriber.get();
        readRequested = false;
        if (sub == null) {
            //TODO: log
            buf.release();
            return;
        }
        sub.onNext(buf.copy().nioBuffer());
        buf.release();
        if (swallowStart != null && clock.get().isAfter(swallowStart.plus(maxSwallow))) {
            pendingReads = 0;
            channel.close();
        } else if (pendingReads > 0 && !readRequested) {
            readRequested = true;
            pendingReads--;
            channel.read();
            channel.eventLoop().submit(channel::read);
        } else {
            //TODO: log
        }
    }

    void complete(ByteBuf buf) {
        if (!channel.eventLoop().inEventLoop()) {
            throw new IllegalStateException("complete() should only come from the event loop");
        }

        closed = true;
        var sub = subscriber.getAndSet(null);
        if (sub == null) {
            //TODO: log
            buf.release();
        } else {
            sub.onNext(buf.copy().nioBuffer());
            buf.release();
            sub.onComplete();
        }
    }

    private void error(Throwable throwable) {
        closed = true;
        closedException = throwable;
        var sub = subscriber.getAndSet(null);
        if (sub != null) {
            sub.onError(throwable);
        } else {
            // TODO: log
        }
        swallowRemainingRequestData();
    }

    private void swallowRemainingRequestData() {
        swallowStart = clock.get();
        pendingReads = Long.MAX_VALUE;
        channel.read();
    }
}
