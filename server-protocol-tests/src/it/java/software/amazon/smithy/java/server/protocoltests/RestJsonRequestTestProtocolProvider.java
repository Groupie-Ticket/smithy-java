/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocoltests;

import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import software.amazon.smithy.java.server.Service;
import software.amazon.smithy.java.server.core.ByteValue;
import software.amazon.smithy.java.server.core.Job;
import software.amazon.smithy.java.server.core.ResolutionRequest;
import software.amazon.smithy.java.server.core.ResolutionResult;
import software.amazon.smithy.java.server.core.ServerProtocol;
import software.amazon.smithy.java.server.core.ServerProtocolProvider;
import software.amazon.smithy.java.server.core.attributes.HttpAttributes;
import software.amazon.smithy.java.server.protocols.restjson.RestJsonProtocol;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * This skips response serialization to avoid noisy logs due to the protocol's inability to serialize a response.
 */
public final class RestJsonRequestTestProtocolProvider implements
    ServerProtocolProvider<RestJsonRequestTestProtocolProvider.RestJsonRequestTestProtocol> {

    @Override
    public RestJsonRequestTestProtocol provideProtocolHandler(Service service) {
        return new RestJsonRequestTestProtocol(service);
    }

    @Override
    public ShapeId getProtocolId() {
        return ShapeId.from("aws.protocols#restJson1ForRequestTests");
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }

    public static final class RestJsonRequestTestProtocol extends ServerProtocol {

        private final RestJsonProtocol delegate;

        private RestJsonRequestTestProtocol(Service service) {
            super(service);
            delegate = new RestJsonProtocol(service);
        }

        @Override
        public ShapeId getProtocolId() {
            return delegate.getProtocolId();
        }

        @Override
        public ResolutionResult resolveOperation(ResolutionRequest request) {
            var result = delegate.resolveOperation(request);
            if (result != null) {
                return new ResolutionResult(result.operation(), this, result.resolutionContext());
            }
            return null;
        }

        @Override
        public CompletableFuture<Void> deserializeInput(Job job) {
            return delegate.deserializeInput(job);
        }

        @Override
        public CompletableFuture<Void> serializeOutput(Job job) {
            job.reply().setValue(new ByteValue("{}".getBytes(StandardCharsets.UTF_8)));
            job.reply()
                .context()
                .put(
                    HttpAttributes.HTTP_HEADERS,
                    HttpHeaders.of(
                        Map.of(
                            "Content-Length",
                            List.of("2")
                        ),
                        (v1, v2) -> true
                    )
                );
            job.reply().context().put(HttpAttributes.STATUS_CODE, 200);
            return CompletableFuture.completedFuture(null);
        }
    }
}
