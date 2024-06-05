/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import java.util.concurrent.CompletableFuture;
import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;
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
        Operation operation = job.getRequest().getContext().get(ServiceAttributes.OPERATION);
        ShapeValue<? extends SerializableStruct> requestValue = job.getRequest().getValue();
        SerializableStruct input = requestValue.get();
        CompletableFuture<Void> cf = new CompletableFuture<>();
        if (operation.isAsync()) {
            CompletableFuture<? extends SerializableStruct> response = (CompletableFuture<? extends SerializableStruct>) operation
                .asyncFunction()
                .apply(input, null);
            response.whenComplete((result, error) -> {
                job.getReply().setValue(new ShapeValue<>(result));
                cf.complete(null);
            });
        } else {
            SerializableStruct output = (SerializableStruct) operation.function().apply(input, null);
            job.getReply().setValue(new ShapeValue<>(output));
            cf.complete(null);
        }
        return cf;
    }

    @Override
    public CompletableFuture<Void> after(Job job) {
        return CompletableFuture.completedFuture(null);
    }
}
