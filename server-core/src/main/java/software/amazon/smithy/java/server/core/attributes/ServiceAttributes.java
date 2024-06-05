/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core.attributes;

import software.amazon.smithy.java.runtime.core.Context;
import software.amazon.smithy.java.server.Operation;


public class ServiceAttributes {

    private ServiceAttributes() {}

    public static final Context.Key<Operation<?, ?>> OPERATION = Context.key("operation");
}
