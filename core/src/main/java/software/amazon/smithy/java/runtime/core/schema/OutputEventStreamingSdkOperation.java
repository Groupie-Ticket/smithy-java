/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core.schema;

public interface OutputEventStreamingSdkOperation<I extends SerializableStruct, O extends SerializableStruct, OS extends SerializableStruct>
    extends ApiOperation<I, O> {

    ShapeBuilder<OS> outputEventBuilder();

    Schema outputEventSchema();
}
