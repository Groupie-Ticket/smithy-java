/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.restjson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import software.amazon.smithy.java.server.Operation;
import software.amazon.smithy.java.server.Service;
import software.amazon.smithy.java.server.core.Job;
import software.amazon.smithy.java.server.core.ServerProtocolHandler;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpTrait;

public class RestJsonProtocolHandler extends ServerProtocolHandler {

    private final Map<UriPattern, Operation> operationMap = new HashMap<>();

    public RestJsonProtocolHandler(Service service) {
        List<Operation<?, ?>> operations = service.getAllOperations();
        for (Operation<?, ?> operation : operations) {
            var httpTrait = operation.getSdkOperation().schema().expectTrait(HttpTrait.class);
            operationMap.put(httpTrait.getUri(), operation);
        }

    }

    @Override
    public ShapeId getProtocolId() {
        return ShapeId.from("aws.protocols#restJson1");
    }

    @Override
    public CompletableFuture<Void> before(Job job) {
        return null;
    }

    @Override
    public CompletableFuture<Void> after(Job job) {
        return null;
    }
}
