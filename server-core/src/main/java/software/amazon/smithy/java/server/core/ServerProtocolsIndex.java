/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.smithy.model.shapes.ShapeId;

public class ServerProtocolsIndex {

    private final static Map<ShapeId, ServerProtocolHandlerProvider> SERVER_PROTOCOL_HANDLERS = ServiceLoader.load(
        ServerProtocolHandlerProvider.class,
        ServerProtocolsIndex.class.getClassLoader()
    )
        .stream()
        .map(ServiceLoader.Provider::get)
        .collect(Collectors.toMap(ServerProtocolHandlerProvider::getProtocolId, Function.identity()));

    public static ServerProtocolHandlerProvider getServerProtocolHandler(ShapeId shapeId) {
        return SERVER_PROTOCOL_HANDLERS.get(shapeId);
    }
}
