/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import software.amazon.smithy.java.runtime.core.Context;

public sealed interface Request permits RequestImpl {

    String getRequestId();

    Context getContext();

    <T extends Value> Value getValue();

    <T extends Value> void setValue(T value);


}
