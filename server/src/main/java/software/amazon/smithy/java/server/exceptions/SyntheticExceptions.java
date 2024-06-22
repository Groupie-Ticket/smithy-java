package software.amazon.smithy.java.server.exceptions;

import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.java.runtime.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;

public final class SyntheticExceptions {
    private static final Map<ShapeId, Schema> SCHEMAS = Map.of(
        UnknownOperationException.SCHEMA.id(),
        UnknownOperationException.SCHEMA,
        SerializationException.SCHEMA.id(),
        SerializationException.SCHEMA,
        InternalServerException.SCHEMA.id(),
        InternalServerException.SCHEMA
    );

    private SyntheticExceptions() {}

    public static Optional<Schema> getSchema(ShapeId shapeId) {
        return Optional.ofNullable(SCHEMAS.get(shapeId));
    }
}
