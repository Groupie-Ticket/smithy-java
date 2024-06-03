/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.netty;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.java.server.Server;
import software.amazon.smithy.java.server.ServerBuilder;
import software.amazon.smithy.java.server.Service;

public final class NettyServerBuilder implements ServerBuilder<NettyServerBuilder> {

    final List<Service> services = new ArrayList<>();
    final URI defaultEndpoint;
    int numWorkers = Runtime.getRuntime().availableProcessors() + 1;

    public NettyServerBuilder(URI defaultEndpoint) {
        this.defaultEndpoint = defaultEndpoint;

    }

    @Override
    public Server build() {
        return new NettyServer(this);
    }

    @Override
    public NettyServerBuilder addService(Service service) {
        services.add(service);
        return this;
    }

}
