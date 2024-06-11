/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core.serde;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;

public final class EventStreamDecodingProcessor<F extends Frame<?>, T extends SerializableStruct>
    implements Flow.Processor<F, T> {

    private final AtomicReference<Flow.Subscriber<? super T>> subscriber = new AtomicReference<>();
    private final ReentrantLock demandLock = new ReentrantLock();
    private final EventDecoder<F, T> decoder;
    private volatile Flow.Subscription subscription;
    private long demand = 0;

    public EventStreamDecodingProcessor(Flow.Publisher<F> publisher, EventDecoder<F, T> decoder) {
        this.decoder = decoder;
        publisher.subscribe(this);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        if (!this.subscriber.compareAndSet(null, subscriber)) {
            throw new IllegalStateException("Can only subscribe once");
        }

        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
                if (n <= 0) {
                    throw new IllegalArgumentException("requested " + n + " frames");
                }

                demandLock.lock();
                try {
                    var oldDemand = demand;
                    try {
                        demand = Math.addExact(demand, n);
                    } catch (ArithmeticException ignore) {
                        demand = Long.MAX_VALUE;
                    }

                    if (oldDemand == 0 && demand > 0 && subscription != null) {
                        subscription.request(1);
                    }
                } finally {
                    demandLock.unlock();
                }
            }

            @Override
            public void cancel() {
                var sub = subscription;
                if (sub != null) {
                    sub.cancel();
                }
            }
        });
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        demandLock.lock();
        try {
            if (this.subscription != null) {
                throw new IllegalStateException("Should only receive onSubscribe once");
            }
            this.subscription = subscription;

            if (demand > 0) {
                subscription.request(1);
            }
        } finally {
            demandLock.unlock();
        }
    }

    @Override
    public void onNext(F frame) {
        demandLock.lock();
        try {
            subscriber.get().onNext(decoder.decode(frame));

            if (--demand > 0) {
                subscription.request(1);
            }
        } finally {
            demandLock.unlock();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        var sub = subscriber.get();
        if (sub != null) {
            sub.onError(throwable);
            sub.onComplete();
        }
    }

    @Override
    public void onComplete() {
        var sub = subscriber.get();
        if (sub != null) {
            sub.onComplete();
        }
    }
}
