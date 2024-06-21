/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocoltests;

import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;
import software.amazon.smithy.java.server.RequestContext;
import software.amazon.smithy.model.shapes.ShapeId;

final class MockOperation implements BiFunction<Object, RequestContext, Object> {
    private static volatile ShapeId lastOperation;

    private final ShapeId operationId;
    private volatile Object lastRequest;
    private volatile Object response = null;

    MockOperation(ShapeId operationId) {
        this.operationId = operationId;
    }

    void setResponse(Object o) {
        response = o;
    }

    Supplier<Object> expectRequest() {
        return () -> {
            if (lastOperation != operationId) {
                Assertions.fail("Expected " + operationId + " to be invoked, was " + lastOperation);
            }
            try {
                return lastRequest;
            } finally {
                lastOperation = null;
            }
        };
    }

    @Override
    public Object apply(Object o, RequestContext requestContext) {
        lastOperation = operationId;
        lastRequest = Normalizer.normalize(o);
        return response;
    }
}
