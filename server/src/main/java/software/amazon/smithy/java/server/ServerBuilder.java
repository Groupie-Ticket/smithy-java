/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server;


public interface ServerBuilder<T extends ServerBuilder<T>> {

    ServerBuilder<T> addService(Service service);

    Server build();
}
