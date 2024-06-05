/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.server.generators;

import java.util.List;
import java.util.Map;
import software.amazon.smithy.aws.traits.protocols.AwsProtocolTrait;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.java.codegen.CodeGenerationContext;
import software.amazon.smithy.java.codegen.writer.JavaWriter;
import software.amazon.smithy.java.server.ServiceSchema;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.protocol.traits.Rpcv2CborTrait;

public record ServiceSchemaGenerator(
    JavaWriter writer,
    ServiceShape shape,
    SymbolProvider symbolProvider,
    Model model,
    CodeGenerationContext context
) implements Runnable {

    @Override
    public void run() {
        List<String> supportedProtocols = shape.getAllTraits()
            .entrySet()
            .stream()
            .filter(k -> k.getValue() instanceof AwsProtocolTrait || k.getValue() instanceof Rpcv2CborTrait)
            .map(Map.Entry::getKey)
            .map(ShapeId::toString)
            .toList();
        writer.pushState();
        writer.putContext("schemaClass", ServiceSchema.class);
        writer.putContext("shapeId", shape.toShapeId());
        writer.putContext("list", List.class);
        writer.putContext("protocols", supportedProtocols);
        writer.write("""
            ${schemaClass:T}.builder()
                .id(ShapeId.from(${shapeId:S}))
                .supportedProtocols(${list:T}.of(${#protocols}"${value:L}"${^key.last}, ${/key.last}${/protocols}).stream().map(ShapeId::from).toList())
                .build();
            """);
        writer.popState();
    }
}
