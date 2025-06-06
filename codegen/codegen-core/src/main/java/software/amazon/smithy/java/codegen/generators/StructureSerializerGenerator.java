/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.generators;

import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.directed.ContextualDirective;
import software.amazon.smithy.java.codegen.CodeGenerationContext;
import software.amazon.smithy.java.codegen.CodegenUtils;
import software.amazon.smithy.java.codegen.writer.JavaWriter;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableShape;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ErrorTrait;

/**
 * Generates the implementation of the
 * {@link SerializableShape#serialize(ShapeSerializer)}
 * method for a structure class.
 */
record StructureSerializerGenerator(
        ContextualDirective<CodeGenerationContext, ?> directive,
        JavaWriter writer,
        StructureShape shape,
        SymbolProvider symbolProvider,
        Model model,
        ServiceShape service) implements Runnable {

    @Override
    public void run() {
        writer.pushState();
        var template = """
                ${^isError}@Override
                public ${schemaClass:N} schema() {
                    return $$SCHEMA;
                }

                ${/isError}
                @Override
                public void serializeMembers(${shapeSerializer:N} serializer) {
                    ${writeMemberSerialization:C|}
                }
                """;
        writer.putContext("shapeSerializer", ShapeSerializer.class);
        writer.putContext("writeMemberSerialization", writer.consumer(this::writeMemberSerialization));
        writer.putContext("schemaClass", Schema.class);
        writer.putContext("isError", shape.hasTrait(ErrorTrait.class));
        writer.write(template);
        writer.popState();
    }

    private void writeMemberSerialization(JavaWriter writer) {
        boolean isError = shape.hasTrait(ErrorTrait.class);

        for (var member : shape.members()) {
            var memberName = symbolProvider.toMemberName(member);
            // if the shape is an error we need to use the `getMessage()` method for message field.
            if (isError && memberName.equalsIgnoreCase("message")) {
                memberName = "getMessage()";
            }

            var target = model.expectShape(member.getTarget());

            writer.pushState();
            writer.putContext(
                    "nullable",
                    CodegenUtils.isNullableMember(model, member)
                            || target.isStructureShape()
                            || target.isUnionShape());
            writer.putContext("memberName", memberName);
            writer.writeInline("""
                    ${?nullable}if (${memberName:L} != null) {
                        ${/nullable}${C|};${?nullable}
                    }${/nullable}
                    """, new SerializerMemberGenerator(directive, writer, member, memberName));
            writer.popState();
        }
    }
}
