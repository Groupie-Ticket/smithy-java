/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;

public final class ShapeValue<T extends SerializableStruct> implements Value<T> {

    private final T value;

    public ShapeValue(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }
}
