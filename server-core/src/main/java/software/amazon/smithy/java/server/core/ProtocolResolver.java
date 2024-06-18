/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.smithy.java.server.Operation;
import software.amazon.smithy.java.server.Service;
import software.amazon.smithy.java.server.exceptions.UnknownOperationException;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.Pair;

public final class ProtocolResolver {

    private final static Map<ShapeId, ServerProtocolProvider> SERVER_PROTOCOL_HANDLERS = ServiceLoader.load(
        ServerProtocolProvider.class,
        ProtocolResolver.class.getClassLoader()
    )
        .stream()
        .map(ServiceLoader.Provider::get)
        .collect(Collectors.toMap(ServerProtocolProvider::getProtocolId, Function.identity()));

    private final List<ServerProtocol> serverProtocolHandlers;

    public ProtocolResolver(Service service) {
        serverProtocolHandlers = SERVER_PROTOCOL_HANDLERS.values()
            .stream()
            .sorted(Comparator.comparing(ServerProtocolProvider::priority))
            .map(p -> p.provideProtocolHandler(service))
            .toList();
    }

    public Pair<Operation<?, ?>, ServerProtocol> resolveOperation(ResolutionRequest request) {
        for (ServerProtocol serverProtocolHandler : serverProtocolHandlers) {
            var operation = serverProtocolHandler.resolveOperation(request);
            if (operation != null) {
                return Pair.of(operation, serverProtocolHandler);
            }
        }
        throw new UnknownOperationException("Unable to resolve operation");
    }

}
