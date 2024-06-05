/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import java.util.concurrent.CompletableFuture;
import software.amazon.smithy.java.server.Operation;
import software.amazon.smithy.java.server.Service;
import software.amazon.smithy.java.server.core.attributes.ServiceAttributes;


public class OperationHandler implements Handler {

    private final Service service;

    public OperationHandler(Service service) {
        this.service = service;
    }

    @Override
    public CompletableFuture<Void> before(Job job) {
        Operation<?, ?> operation = job.getRequest().getContext().get(ServiceAttributes.OPERATION);
        CompletableFuture<Void> cf = new CompletableFuture<>();
        if (operation.isAsync()) {
            operation.asyncFunction().apply(null, null).thenRun(() -> cf.complete(null));
        } else {
            operation.function().apply(null, null);
            cf.complete(null);
        }
        return cf;
    }

    @Override
    public CompletableFuture<Void> after(Job job) {
        return CompletableFuture.completedFuture(null);
    }
}
