/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.restjson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;

public class ReactiveInputStreamAdapter extends InputStream {
    private sealed interface Event {}

    private record SubEvent(Flow.Subscription sub) implements Event {}

    private record DataEvent(ByteBuffer buf) implements Event {}

    private record FailureEvent(Throwable failure) implements Event {}

    private record CompleteEvent() implements Event {}


    private final BlockingQueue<Event> queue = new LinkedBlockingQueue<>();
    private volatile Flow.Subscription subscription;
    private ByteBuffer currentBuf;
    private boolean complete;

    public ReactiveInputStreamAdapter(Flow.Publisher<ByteBuffer> publisher) {
        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription sub) {
                queue.add(new SubEvent(sub));
            }

            @Override
            public void onNext(ByteBuffer item) {
                queue.add(new DataEvent(item));
            }

            @Override
            public void onError(Throwable throwable) {
                queue.add(new FailureEvent(throwable));
            }

            @Override
            public void onComplete() {
                queue.add(new CompleteEvent());
            }
        });
    }

    @Override
    public int read() throws IOException {
        if (complete) {
            throw new IllegalStateException("read from closed stream");
        }

        ByteBuffer buf = getCurrentBuf();
        if (buf == null) {
            return -1;
        }

        return buf.get();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (complete) {
            throw new IllegalStateException("read from closed stream");
        }

        ByteBuffer buf = getCurrentBuf();
        if (buf == null) {
            return -1;
        }
        int ret = Math.min(len, available(buf));
        buf.get(b, off, ret);
        return ret;
    }

    private ByteBuffer getCurrentBuf() throws IOException {
        if (available(currentBuf) > 0) {
            return currentBuf;
        }
        Event newEvent = queue.poll();

        // If there's nothing new in the queue, send an upstream request and block
        // until something new happens
        if (newEvent == null) {
            // It's possible for this to submit multiple requests if the thread calling read()
            // is interrupted before a new event is ready. Worst case, we just buffer an additional
            // ByteBuf or two.
            if (subscription != null) {
                subscription.request(1);
            }
            try {
                newEvent = queue.take();
            } catch (InterruptedException e) {
                throw new IOException("Interrupted while waiting for next bytes", e);
            }
        }

        if (newEvent instanceof SubEvent subEvent) {
            subscription = subEvent.sub;
            return getCurrentBuf();
        }

        if (newEvent instanceof CompleteEvent) {
            complete = true;
            return null;
        }

        if (newEvent instanceof FailureEvent failureEvent) {
            throw new IOException("Failed while reading bytes", failureEvent.failure);
        }

        currentBuf = ((DataEvent) newEvent).buf;
        return currentBuf;
    }

    private int available(ByteBuffer buffer) {
        if (buffer == null) {
            return 0;
        }
        return buffer.remaining();
    }

    @Override
    public void close() throws IOException {
        complete = true;
        subscription.cancel();
        queue.clear();
        currentBuf = null;
        subscription = null;
    }
}
