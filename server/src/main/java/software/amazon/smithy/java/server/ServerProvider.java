/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server;

import java.net.URI;

public interface ServerProvider {

    String name();

    ServerBuilder<?> newBuilder(URI defaultEndpoint);
}
