/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import software.amazon.smithy.java.runtime.core.Context;
import software.amazon.smithy.java.server.RequestContext;

public class RequestContextImpl implements RequestContext {

    private final String requestId;

    public RequestContextImpl(String requestId, Context context) {
        this.requestId = requestId;
    }

    @Override
    public String getRequestId() {
        return requestId;
    }
}
