/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocoltests;

import software.amazon.smithy.java.server.Operation;
import software.amazon.smithy.java.server.Service;
import software.amazon.smithy.java.server.core.Job;
import software.amazon.smithy.java.server.core.ResolutionRequest;
import software.amazon.smithy.java.server.core.ServerProtocol;
import software.amazon.smithy.java.server.core.ServerProtocolProvider;
import software.amazon.smithy.java.server.core.ShapeValue;
import software.amazon.smithy.java.server.protocols.restjson.RestJsonProtocol;
import software.amazon.smithy.model.shapes.ShapeId;

public class RestJsonResponseTestProtocolProvider implements
    ServerProtocolProvider<RestJsonResponseTestProtocolProvider.RestJsonResponseTestProtocol> {


    @Override
    public RestJsonResponseTestProtocol provideProtocolHandler(Service service) {
        return new RestJsonResponseTestProtocol(service);
    }

    @Override
    public ShapeId getProtocolId() {
        return ShapeId.from("aws.protocols#restJson1ForResponseTests");
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }

    public static final class RestJsonResponseTestProtocol extends ServerProtocol {

        private final RestJsonProtocol delegate;

        private RestJsonResponseTestProtocol(Service service) {
            super(service);
            delegate = new RestJsonProtocol(service);
        }

        @Override
        public ShapeId getProtocolId() {
            return delegate.getProtocolId();
        }

        @Override
        public Operation<?, ?> resolveOperation(ResolutionRequest request) {
            ShapeId serviceId = request.getHeaders()
                .firstValue("x-protocol-test-service")
                .map(ShapeId::from)
                .orElse(ShapeId.from("unknown#service"));
            ShapeId operationId = request.getHeaders()
                .firstValue("x-protocol-test-operation")
                .map(ShapeId::from)
                .orElse(ShapeId.from("unknown#service"));

            if (!getService().getSchema().getId().equals(serviceId)) {
                return null;
            }

            return getOperations().stream()
                .filter(operation -> operation.getApiOperation().schema().id().equals(operationId))
                .findFirst()
                .orElse(null);
        }

        @Override
        public void deserializeInput(Job job) {
            job.request().setValue(new ShapeValue(null));
        }

        @Override
        public void serializeOutput(Job job) {
            delegate.serializeOutput(job);
        }
    }
}
