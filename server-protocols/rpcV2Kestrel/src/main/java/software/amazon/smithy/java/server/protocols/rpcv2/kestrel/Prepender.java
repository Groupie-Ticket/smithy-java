/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.rpcv2.kestrel;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

final class Prepender<T> implements Flow.Processor<T, T> {
    private final Flow.Publisher<T> delegate;
    private final T firstItem;
    private Flow.Subscriber<? super T> subscriber;

    Prepender(Flow.Publisher<T> delegate, T firstItem) {
        this.delegate = delegate;
        this.firstItem = firstItem;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> s) {
        subscriber = s;
        delegate.subscribe(this);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscriber.onSubscribe(new Flow.Subscription() {
            private final AtomicBoolean firstRequest = new AtomicBoolean(true);

            @Override
            public void request(long n) {
                if (n <= 0) {
                    subscription.cancel();
                    subscriber.onError(new IllegalArgumentException("Requested " + n + " items"));
                }

                if (firstRequest.getAndSet(false)) {
                    subscriber.onNext(firstItem);
                    if (n - 1 > 0) {
                        subscription.request(n - 1);
                    }
                } else {
                    subscription.request(n);
                }
            }

            @Override
            public void cancel() {
                subscription.cancel();
            }
        });
    }

    @Override
    public void onNext(T item) {
        subscriber.onNext(item);
    }

    @Override
    public void onError(Throwable t) {
        subscriber.onError(t);
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
    }
}
