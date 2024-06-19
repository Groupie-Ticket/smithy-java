/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.server.generators;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import software.amazon.smithy.aws.traits.auth.SigV4ATrait;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.aws.traits.protocols.AwsProtocolTrait;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.java.codegen.CodeGenerationContext;
import software.amazon.smithy.java.codegen.writer.JavaWriter;
import software.amazon.smithy.java.server.ServiceSchema;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
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
        var supportedProtocols = filterTraits(shape, AwsProtocolTrait.class, Rpcv2CborTrait.class);
        var awsAuthModes = filterTraits(shape, SigV4Trait.class, SigV4ATrait.class);
        writer.pushState();
        writer.putContext("schemaClass", ServiceSchema.class);
        writer.putContext("shapeId", shape.toShapeId());
        writer.putContext("stream", Stream.class);
        writer.putContext("protocols", supportedProtocols);
        writer.putContext("awsAuthModes", awsAuthModes);
        writer.write(
            """
                ${schemaClass:T}.builder()
                        .id(ShapeId.from(${shapeId:S}))
                        .supportedProtocols(${stream:T}.of(${#protocols}"${value:L}"${^key.last},${/key.last}${/protocols}).map(ShapeId::from).toList())
                        .awsAuthModes(${stream:T}.${^awsAuthModes}empty()${/awsAuthModes}${#awsAuthModes}of("${value:L}"${^key.last},${/key.last}${/awsAuthModes}).map(ShapeId::from).toList())
                        .build();
                """
        );
        writer.popState();
    }

    private static List<String> filterTraits(Shape shape, Class<?>... classes) {
        return shape.getAllTraits()
            .entrySet()
            .stream()
            .filter(k -> anyInstanceOf(k.getValue(), classes))
            .map(Map.Entry::getKey)
            .map(ShapeId::toString)
            .toList();
    }

    private static boolean anyInstanceOf(Object o, Class<?>... classes) {
        for (Class<?> c : classes) {
            if (c.isInstance(o)) {
                return true;
            }
        }

        return false;
    }
}
