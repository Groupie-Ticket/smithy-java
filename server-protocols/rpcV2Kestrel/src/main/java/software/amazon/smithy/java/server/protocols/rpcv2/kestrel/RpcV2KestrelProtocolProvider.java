/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.rpcv2.kestrel;

import software.amazon.smithy.java.server.Service;
import software.amazon.smithy.java.server.core.ServerProtocolProvider;
import software.amazon.smithy.model.shapes.ShapeId;

public class RpcV2KestrelProtocolProvider implements ServerProtocolProvider<RpcV2KestrelProtocol> {
    @Override
    public RpcV2KestrelProtocol provideProtocolHandler(Service service) {
        return new RpcV2KestrelProtocol(service);
    }

    @Override
    public ShapeId getProtocolId() {
        return ShapeId.from("smithy.protocols#kestrel");
    }

    @Override
    public int priority() {
        return 1;
    }
}
