/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import software.amazon.smithy.java.runtime.core.Context;
import software.amazon.smithy.java.server.Operation;
import software.amazon.smithy.model.shapes.ShapeId;

public abstract class ServerProtocol {

    public static Context.Key<ServerProtocol> SERVER_PROTOCOL = Context.key("server-protocol");


    public abstract ShapeId getProtocolId();

    /**
     * Implementations are supposed to resolve the service and operation and claim the job.
     *
     * @param
     * @return
     */
    public abstract Operation<?, ?> resolveOperation(ResolutionRequest request);

    public abstract void deserializeInput(Job job);

    public abstract void serializeOutput(Job job);


}
