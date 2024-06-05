/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.smithy.java.runtime.core.Context;

public final class JobImpl implements Job {

    private final Request request;
    private final Reply reply;
    private final Context context;
    private final AtomicBoolean isDone = new AtomicBoolean(false);
    private final AtomicReference<Throwable> failure = new AtomicReference<>();

    public JobImpl(Request request, Reply reply) {
        this.request = request;
        this.reply = reply;
        this.context = Context.create();
    }

    @Override
    public Request getRequest() {
        return request;
    }

    @Override
    public Reply getReply() {
        return reply;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public boolean isDone() {
        return isDone.get();
    }

    @Override
    public void setDone() {
        isDone.set(true);
    }

    @Override
    public Optional<Throwable> getFailure() {
        return Optional.ofNullable(failure.get());
    }

    @Override
    public void setFailure(Throwable t) {
        failure.set(t);
    }
}
