/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.netty;

import java.net.URI;
import software.amazon.smithy.java.server.ServerBuilder;
import software.amazon.smithy.java.server.ServerProvider;

public class NettyServerProvider implements ServerProvider {

    @Override
    public String name() {
        return "netty";
    }

    @Override
    public ServerBuilder<?> newBuilder(URI defaultEndpoint) {
        return new NettyServerBuilder(defaultEndpoint);
    }
}
