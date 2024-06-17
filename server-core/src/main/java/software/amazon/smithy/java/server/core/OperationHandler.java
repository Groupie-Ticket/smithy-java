/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import java.util.concurrent.CompletableFuture;
import software.amazon.smithy.java.runtime.core.schema.ModeledApiException;
import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;
import software.amazon.smithy.java.server.Operation;
import software.amazon.smithy.java.server.Service;
import software.amazon.smithy.java.server.exceptions.InternalServerException;


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
                .apply(input, job.request().userContext());
            response.whenComplete((result, error) -> {
                SerializableStruct output;
                if (error != null) {
                    if (error instanceof ModeledApiException e) {
                        output = e;
                    } else {
                        output = new InternalServerException(error);
                    }
                } else {
                    output = result;
                }
                job.reply().setValue(new ShapeValue<>(output));
                cf.complete(null);
            });
        } else {
            SerializableStruct output;
            try {
                output = (SerializableStruct) operation.function()
                    .apply(input, job.request().userContext());
            } catch (ModeledApiException exception) {
                output = exception;
            } catch (Throwable throwable) {
                output = new InternalServerException(throwable);
            }
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
