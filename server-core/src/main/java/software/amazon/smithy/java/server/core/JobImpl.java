/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import software.amazon.smithy.java.runtime.core.Context;

public final class JobImpl implements Job {

    private final Request request;
    private final Reply reply;
    private final Context context;

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
}
