/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core.serde;

import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;

public interface EventEncoder<T extends SerializableStruct, F extends Frame<?>> {

    F encode(T item);

}
