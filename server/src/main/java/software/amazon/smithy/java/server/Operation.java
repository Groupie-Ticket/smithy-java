/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server;

import java.util.function.BiFunction;

public record Operation<I, O>(String name, boolean isAsync, BiFunction<I, RequestContext, O> function) {
}
