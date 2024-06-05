/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import software.amazon.smithy.java.server.Service;
import software.amazon.smithy.model.shapes.ShapeId;

public interface ServerProtocolHandlerProvider<T extends ServerProtocolHandler> {

    T provideProtocolHandler(Service service);

    ShapeId getProtocolId();
}
