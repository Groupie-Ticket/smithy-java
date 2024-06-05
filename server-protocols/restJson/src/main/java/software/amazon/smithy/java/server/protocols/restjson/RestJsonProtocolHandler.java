/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.restjson;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.java.server.Operation;
import software.amazon.smithy.java.server.Service;
import software.amazon.smithy.java.server.core.ByteValue;
import software.amazon.smithy.java.server.core.Job;
import software.amazon.smithy.java.server.core.ServerProtocolHandler;
import software.amazon.smithy.java.server.core.attributes.HttpAttributes;
import software.amazon.smithy.java.server.core.attributes.ServiceAttributes;
import software.amazon.smithy.java.server.exceptions.UnknownOperationException;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpTrait;

final class RestJsonProtocolHandler extends ServerProtocolHandler {

    private final List<Operation<?, ?>> operations = new ArrayList<>();

    public RestJsonProtocolHandler(final Service service) {
        this.operations.addAll(service.getAllOperations());
    }

    @Override
    public ShapeId getProtocolId() {
        return ShapeId.from("aws.protocols#restJson1");
    }

    @Override
    public void doBefore(Job job) {
        if (!claim(job)) {
            return;
        }

        UriPattern uri = UriPattern.parse(job.getRequest().getContext().get(HttpAttributes.HTTP_URI).getPath());
        Operation<?, ?> selectedOperation = null;
        for (Operation<?, ?> operation : operations) {
            UriPattern uriPattern = operation.getSdkOperation().schema().expectTrait(HttpTrait.class).getUri();
            if (uriPattern.equals(uri)) {
                selectedOperation = operation;
                break;
            }
        }

        if (selectedOperation == null) {
            throw new UnknownOperationException("Unknown operation: " + uri);
        }

        job.getRequest().getContext().put(ServiceAttributes.OPERATION, selectedOperation);

    }

    @Override
    public void doAfter(Job job) {
        if (!isClaimedByThis(job)) {
            return;
        }
        job.getReply()
            .setValue(
                new ByteValue(
                    job.getRequest()
                        .getContext()
                        .get(ServiceAttributes.OPERATION)
                        .name()
                        .getBytes(StandardCharsets.UTF_8)
                )
            );
    }
}
