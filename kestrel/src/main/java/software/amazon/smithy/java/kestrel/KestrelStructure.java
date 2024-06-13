/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.kestrel;

public interface KestrelStructure<T> extends KestrelObject {
    Class<? extends T> getConvertedType();

    T convertTo();

    void convertFrom(T object);
}
