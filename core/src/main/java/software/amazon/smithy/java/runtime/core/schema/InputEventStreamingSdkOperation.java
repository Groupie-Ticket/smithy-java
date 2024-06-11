/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core.schema;

public interface InputEventStreamingSdkOperation<I extends SerializableStruct, O extends SerializableStruct, IS extends SerializableStruct>
    extends ApiOperation<I, O> {

    ShapeBuilder<IS> inputEventBuilder();

    Schema inputEventSchema();
}
