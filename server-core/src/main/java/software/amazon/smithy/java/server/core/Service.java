/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

public interface Service {

    /**
     * Returns {@link Operation} for a given unqualified operation name.
     * @param operationName Unqualified operation name.
     * @return {@link Operation}
     */
    Operation<?, ?> getOperation(String operationName);


}
