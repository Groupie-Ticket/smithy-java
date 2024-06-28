/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.rpcv2.sparrowhawk;

import software.amazon.smithy.java.server.Service;
import software.amazon.smithy.java.server.core.ServerProtocolProvider;
import software.amazon.smithy.model.shapes.ShapeId;

public class RpcV2SparrowhawkProtocolProvider implements ServerProtocolProvider<RpcV2SparrowhawkProtocol> {
    @Override
    public RpcV2SparrowhawkProtocol provideProtocolHandler(Service service) {
        return new RpcV2SparrowhawkProtocol(service);
    }

    @Override
    public ShapeId getProtocolId() {
        return ShapeId.from("smithy.protocols#sparrowhawk");
    }

    @Override
    public int priority() {
        return 1;
    }
}
