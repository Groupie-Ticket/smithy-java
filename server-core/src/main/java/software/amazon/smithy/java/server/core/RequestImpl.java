/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import software.amazon.smithy.java.runtime.core.Context;

public final class RequestImpl implements Request {

    private final String requestId;
    private final Context context;
    private Value value;

    public RequestImpl(String requestId) {
        this.requestId = requestId;
        this.context = Context.create();
    }

    @Override
    public String getRequestId() {
        return requestId;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public <T extends Value> T getValue() {
        return (T) value;
    }

    @Override
    public <T extends Value> void setValue(T value) {
        this.value = value;
    }
}
