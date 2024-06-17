/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.exceptions;

import software.amazon.smithy.java.runtime.core.schema.*;
import software.amazon.smithy.java.runtime.core.serde.ShapeSerializer;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;

public class InternalServerException extends ModeledApiException {

    private static final ShapeId ID = ShapeId.from("smithy.exceptions#InternalServerException");

    private static final Schema SCHEMA_MESSAGE = Schema.memberBuilder("message", PreludeSchemas.STRING)
        .id(ID)
        .build();

    public static final Schema SCHEMA = Schema.builder()
        .id(ID)
        .type(ShapeType.STRUCTURE)
        .traits(
            new ErrorTrait("server"),
            new HttpErrorTrait.Provider().createTrait(
                ShapeId.from("smithy.api#httpError"),
                Node.from(500)
            )
        )
        .members(
            SCHEMA_MESSAGE
        )
        .build();

    public InternalServerException(Throwable cause) {
        super(ID, "Internal Server Error", cause);
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString(SCHEMA_MESSAGE, getMessage());
    }

    @Override
    public void serialize(ShapeSerializer encoder) {
        encoder.writeStruct(SCHEMA, this);
    }
}
