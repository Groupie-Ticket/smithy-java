/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.kestrel.codec;

import software.amazon.smithy.java.kestrel.KestrelObject;
import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;
import software.amazon.smithy.model.shapes.ShapeId;

public interface KestrelStructure<T extends SerializableStruct> extends KestrelObject {
    Class<? extends T> getConvertedType();

    ShapeId getConvertedId();

    T convertTo();
}
