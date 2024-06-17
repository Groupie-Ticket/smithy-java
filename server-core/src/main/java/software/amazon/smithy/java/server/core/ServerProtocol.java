/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import java.util.List;
import software.amazon.smithy.java.server.Operation;
import software.amazon.smithy.java.server.Service;
import software.amazon.smithy.model.shapes.ShapeId;

public abstract class ServerProtocol {

    private final List<Operation<?, ?>> operations;
    private final Service service;

    protected ServerProtocol(Service service) {
        this.service = service;
        this.operations = List.copyOf(service.getAllOperations());
    }


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

    protected List<Operation<?, ?>> getOperations() {
        return operations;
    }

    protected Service getService() {
        return service;
    }


}
