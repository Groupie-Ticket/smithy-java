/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server;

import java.net.URI;
import java.util.Objects;
import java.util.ServiceLoader;

public interface Server {

    static ServerBuilder builder(URI endpoint) {
        return findBuilder(null, endpoint);
    }

    static ServerBuilder<?> builder(String serverName, URI endpoint) {
        return findBuilder(Objects.requireNonNull(serverName, "Server type can't be null"), endpoint);
    }

    private static ServerBuilder<?> findBuilder(String serverName, URI endpoint) {
        ServerBuilder<?> builder = null;
        var iterator = ServiceLoader.load(ServerProvider.class).iterator();
        while (iterator.hasNext()) {
            ServerProvider provider = iterator.next();
            if (serverName != null) {
                if (serverName.equals(provider.name())) {
                    return provider.newBuilder(endpoint);
                } else {
                    continue;
                }
            }
            if (iterator.hasNext()) {
                throw new IllegalStateException(
                    "Server provider has more than one implementation. Specify a particular type"
                );
            }
            return provider.newBuilder(endpoint);
        }
        throw new IllegalStateException("Couldn't find a matching Server implementation");
    }

    void start();

    void stop();
}
