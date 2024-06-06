/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import java.util.concurrent.CompletableFuture;
import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;
import software.amazon.smithy.java.server.Operation;
import software.amazon.smithy.java.server.Service;


public class OperationHandler implements Handler {

    private final Service service;

    public OperationHandler(Service service) {
        this.service = service;
    }

    @Override
    public CompletableFuture<Void> before(Job job) {
        Operation operation = job.operation();
        ShapeValue<? extends SerializableStruct> requestValue = job.request().getValue();
        SerializableStruct input = requestValue.get();
        CompletableFuture<Void> cf = new CompletableFuture<>();
        if (operation.isAsync()) {
            CompletableFuture<? extends SerializableStruct> response = (CompletableFuture<? extends SerializableStruct>) operation
                .asyncFunction()
                .apply(input, null);
            response.whenComplete((result, error) -> {
                job.reply().setValue(new ShapeValue<>(result));
                cf.complete(null);
            });
        } else {
            SerializableStruct output = (SerializableStruct) operation.function().apply(input, null);
            job.reply().setValue(new ShapeValue<>(output));
            cf.complete(null);
        }
        return cf;
    }

    @Override
    public CompletableFuture<Void> after(Job job) {
        return CompletableFuture.completedFuture(null);
    }
}
