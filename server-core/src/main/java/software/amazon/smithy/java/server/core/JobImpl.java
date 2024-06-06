/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.smithy.java.runtime.core.Context;
import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;
import software.amazon.smithy.java.server.Operation;

public final class JobImpl implements Job {

    private final Request request;
    private final Reply reply;
    private final Operation<? extends SerializableStruct, ? extends SerializableStruct> operation;
    private final ServerProtocol protocol;
    private final Context context;
    private final AtomicBoolean isDone = new AtomicBoolean(false);
    private final AtomicReference<Throwable> failure = new AtomicReference<>();

    public JobImpl(
        Request request,
        Reply reply,
        Operation<? extends SerializableStruct, ? extends SerializableStruct> operation,
        ServerProtocol chosenProtocol
    ) {
        this.request = request;
        this.reply = reply;
        this.operation = operation;
        this.protocol = chosenProtocol;
        this.context = Context.create();
    }

    @Override
    public Request request() {
        return request;
    }

    @Override
    public Reply reply() {
        return reply;
    }

    @Override
    public Context context() {
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

    @Override
    public Operation<? extends SerializableStruct, ? extends SerializableStruct> operation() {
        return operation;
    }

    @Override
    public ServerProtocol chosenProtocol() {
        return protocol;
    }
}
