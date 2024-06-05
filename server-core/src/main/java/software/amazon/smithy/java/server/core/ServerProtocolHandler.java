/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import software.amazon.smithy.java.runtime.core.Context;

public abstract class ServerProtocolHandler implements Handler {

    private static final Context.Key<Handler> PROTOCOL_HANDLER = Context.key("protocol-handler");

    public abstract String getProtocolId();

    protected boolean claim(Job job) {
        return job.getContext().putIfAbsent(PROTOCOL_HANDLER, this) == null;
    }

    protected boolean isClaimedByThis(Job job) {
        return job.getContext().get(PROTOCOL_HANDLER) == this;
    }


}
