/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.restjson;

import software.amazon.smithy.java.server.Service;
import software.amazon.smithy.java.server.core.ServerProtocolProvider;
import software.amazon.smithy.model.shapes.ShapeId;

public class RestJsonProtocolProvider implements ServerProtocolProvider<RestJsonProtocol> {

    @Override
    public RestJsonProtocol provideProtocolHandler(Service service) {
        return new RestJsonProtocol(service);
    }

    @Override
    public ShapeId getProtocolId() {
        return ShapeId.from("aws.protocols#restJson1");
    }

    @Override
    public int priority() {
        return 2;
    }
}
