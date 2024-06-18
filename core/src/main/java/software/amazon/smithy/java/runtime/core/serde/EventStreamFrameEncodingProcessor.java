/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core.serde;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;

public final class EventStreamFrameEncodingProcessor<F extends Frame<?>, T extends SerializableStruct> implements
    Flow.Processor<T, ByteBuffer>,
    Flow.Subscription {
    private static final NoInitialEventException COMPLETE = new NoInitialEventException();

    private final AtomicReference<Throwable> terminalEvent = new AtomicReference<>();
    private final AtomicLong pendingRequests = new AtomicLong();
    private final AtomicInteger pendingFlushes = new AtomicInteger();
    private final Flow.Publisher<T> publisher;
    private final EventEncoder<T, F> eventEncoder;
    private final FrameEncoder<F> encoder;
    private final BlockingQueue<ByteBuffer> queue = new LinkedBlockingQueue<>();

    private volatile Flow.Subscription upstreamSubscription;
    private volatile Flow.Subscriber<? super ByteBuffer> downstream;
    private boolean terminated = false;

    public EventStreamFrameEncodingProcessor(
        Flow.Publisher<T> publisher,
        EventEncoder<T, F> eventEncoder,
        FrameEncoder<F> encoder
    ) {
        this.publisher = publisher;
        this.eventEncoder = eventEncoder;
        this.encoder = encoder;
        publisher.subscribe(this);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        upstreamSubscription = subscription;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        downstream = subscriber;
        subscriber.onSubscribe(this);
    }

    @Override
    public void onNext(T item) {
        try {
            queue.add(encoder.encode(eventEncoder.encode(item)));
        } catch (Throwable t) {
            onError(t);
            return;
        }

        flush();
    }

    @Override
    public void onError(Throwable t) {
        upstreamSubscription.cancel();
        terminalEvent.compareAndSet(null, t);
        flush();
    }

    @Override
    public void onComplete() {
        terminalEvent.compareAndSet(null, COMPLETE);
        flush();
    }

    @Override
    public void request(long n) {
        if (n <= 0) {
            onError(new IllegalArgumentException("got a request for " + n + " items"));
            return;
        }

        accumulate(pendingRequests, n);
        flush();
    }

    private void flush() {
        if (pendingFlushes.getAndIncrement() > 0) {
            return;
        }

        if (terminated) {
            return;
        }


        int loop = 1;
        while (loop > 0) {
            long pending = pendingRequests.get();

            Flow.Subscriber<? super ByteBuffer> subscriber = downstream;
            long delivered = sendMessages(subscriber, pending);
            boolean empty = queue.isEmpty();
            Throwable term = terminalEvent.get();
            if (term != null && attemptTermination(subscriber, term, empty)) {
                terminated = true;
                return;
            }

            if (delivered > 0) {
                /*
                 * We still need to re-read at the start of the loop because additions to pendingRequest happen-before
                 * additions to pendingFlushes. If we reused this value in the next loop, there is a race condition:
                 * 1. Thread A is in `flush()`.
                 * 2. Thread B enters `request`.
                 * 3. Thread A decrements pendingRequests here.
                 * 4. Thread B increments pendingFlushes and returns because A is still flushing.
                 * 5. Thread A decrements pendingFlushes and noticed that something requested a flush. It loops around
                 *    but _doesn't_ read the new value of pendingRequests, so it does nothing.
                 *
                 * In short, reload _all_ state on _every_Loop. You are only guaranteed to see updates to shared state
                 * after reading a value from pendingFlushes.
                 */
                accumulate(pendingRequests, -delivered);

                /*
                 * We need to accumulate our local `pending` value separately from the atomic `pendingRequests`.
                 * Consider this scenario with two buffers available for flush and one outstanding request:
                 * 1. Thread A is in `flush()`. It observes 1 pending request and flushes 1 buffer.
                 * 2. Thread B calls `request` and increments `pendingRequests` from 1 to 2.
                 * 3. Thread A enters this if statement. It delivered one message, subtracts 1 from `pendingRequests`,
                 *    and stores the new sum of 1 in `pending`.
                 * 4. Thread A enters the next if statement and requests one buffer from the upstream subscription,
                 *    even though it fulfilled the 1 request that was present in this loop and we have 1 more buffer
                 *    that can be flushed.
                 *
                 * To avoid over-requesting buffers, we must only consider how much demand we successfully fulfilled
                 * verses how much we were willing to fulfill on this loop. To do this, we must only read a value from
                 * `pendingRequests` a single time, at the top of each loop. The rest of the loop must only work with
                 * the value read at the start.
                 */
                pending = accumulate(pending, -delivered);
            }

            if (pending > 0) {
                // do this inside the flush loop so a recursive flush -> request -> onNext -> flush
                // call will be aborted by the `pendingFlushes` check.
                upstreamSubscription.request(1);
            }

            loop = pendingFlushes.addAndGet(-loop);
        }
    }

    /**
     * @return true if this decoder is in a terminal state
     */
    private boolean attemptTermination(Flow.Subscriber<? super ByteBuffer> subscriber, Throwable term, boolean done) {
        if (done && subscriber != null) {
            if (term == COMPLETE) {
                subscriber.onComplete();
            } else {
                subscriber.onNext(encoder.encode(eventEncoder.encodeFailure(term)));
                subscriber.onComplete();
            }
            return true;
        }

        return false;
    }

    /**
     * Tries to flush up to the given demand and signals if we need data from
     * upstream if there is unfulfilled demand.
     *
     * @param outstanding outstanding message demand to fulfill
     * @return number of fulfilled requests
     */
    private long sendMessages(Flow.Subscriber<? super ByteBuffer> subscriber, long outstanding) {
        long served = 0;

        if (subscriber != null) {
            while (served < outstanding) {
                ByteBuffer m = queue.poll();
                if (m == null) {
                    break;
                }
                served++;
                subscriber.onNext(m);
            }
        }

        return served;
    }

    @Override
    public void cancel() {
        upstreamSubscription.cancel();
    }

    private static long accumulate(long current, long n) {
        if (current == Long.MAX_VALUE || n == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        try {
            return Math.addExact(current, n);
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE;
        }
    }

    private static long accumulate(AtomicLong l, long n) {
        return l.accumulateAndGet(n, EventStreamFrameEncodingProcessor::accumulate);
    }
}
