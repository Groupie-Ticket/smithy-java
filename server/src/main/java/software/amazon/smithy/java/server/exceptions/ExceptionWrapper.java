/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.exceptions;

import software.amazon.smithy.java.runtime.core.schema.ModeledApiException;
import software.amazon.smithy.java.runtime.core.schema.Schema;
import software.amazon.smithy.java.runtime.core.serde.ShapeSerializer;
import software.amazon.smithy.model.shapes.ShapeId;

public class ExceptionWrapper extends ModeledApiException {

    private static final ShapeId ID = ShapeId.from("smithy.exceptions#ExceptionWrapper");

    private final ModeledApiException value;
    private final Schema schema;

    public ExceptionWrapper(ModeledApiException exception, Schema schema) {
        super(ID, "Internal Server Error", exception);
        this.value = exception;
        this.schema = schema;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeStruct(schema, value);
    }

    @Override
    public void serialize(ShapeSerializer encoder) {
    }
}
