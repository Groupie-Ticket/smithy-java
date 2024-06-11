/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.generators;

import java.util.function.Consumer;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.directed.GenerateOperationDirective;
import software.amazon.smithy.java.codegen.CodeGenerationContext;
import software.amazon.smithy.java.codegen.JavaCodegenSettings;
import software.amazon.smithy.java.codegen.sections.ClassSection;
import software.amazon.smithy.java.codegen.writer.JavaWriter;
import software.amazon.smithy.java.runtime.core.schema.ApiOperation;
import software.amazon.smithy.java.runtime.core.schema.InputEventStreamingSdkOperation;
import software.amazon.smithy.java.runtime.core.schema.Schema;
import software.amazon.smithy.java.runtime.core.schema.ShapeBuilder;
import software.amazon.smithy.java.runtime.core.schema.TypeRegistry;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.EventStreamIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.StreamingTrait;


public class OperationGenerator
    implements Consumer<GenerateOperationDirective<CodeGenerationContext, JavaCodegenSettings>> {

    @Override
    public void accept(GenerateOperationDirective<CodeGenerationContext, JavaCodegenSettings> directive) {
        accept(directive, directive.symbol());
    }

    public void accept(
        GenerateOperationDirective<CodeGenerationContext, JavaCodegenSettings> directive,
        Symbol symbol
    ) {

        var shape = directive.shape();

        directive.context()
            .writerDelegator()
            .useFileWriter(symbol.getDeclarationFile(), symbol.getNamespace(), writer -> {
                var inputShape = directive.model().expectShape(shape.getInputShape());
                var input = directive.symbolProvider().toSymbol(inputShape);
                var outputShape = directive.model().expectShape(shape.getOutputShape());
                var output = directive.symbolProvider().toSymbol(outputShape);
                writer.pushState(new ClassSection(shape));
                writer.putContext("shape", symbol);
                writer.putContext("sdkOperation", ApiOperation.class);
                writer.putContext("inputType", input);
                writer.putContext("streamingInput", hasStream(directive.model(), inputShape));
                writer.putContext("outputType", output);
                writer.putContext("streamingOutput", hasStream(directive.model(), outputShape));
                writer.putContext("sdkSchema", Schema.class);
                writer.putContext("sdkShapeBuilder", ShapeBuilder.class);
                writer.putContext("typeRegistry", TypeRegistry.class);

                writer.putContext(
                    "operationType",
                    new OperationTypeGenerator(
                        writer,
                        shape,
                        directive.symbolProvider(),
                        directive.model(),
                        directive.context()
                    )
                );

                writer.putContext(
                    "schema",
                    new SchemaGenerator(
                        writer,
                        shape,
                        directive.symbolProvider(),
                        directive.model(),
                        directive.context()
                    )
                );
                writer.putContext(
                    "typeRegistrySection",
                    new TypeRegistryGenerator(
                        writer,
                        shape,
                        directive.symbolProvider(),
                        directive.model(),
                        directive.service()
                    )
                );

                writer.putContext(
                    "inputEventStreamSection",
                    new InputEventStreamGenerator(
                        writer,
                        shape,
                        directive.symbolProvider(),
                        directive.model(),
                        directive.service()
                    )
                );

                writer.write(
                    """
                        public final class ${shape:T} implements ${operationType:C} {

                            static final ${sdkSchema:T} SCHEMA = ${schema:C}

                            ${typeRegistrySection:C|}

                            @Override
                            public ${sdkShapeBuilder:T}<${inputType:T}> inputBuilder() {
                                return ${inputType:T}.builder();
                            }

                            @Override
                            public ${sdkShapeBuilder:T}<${outputType:T}> outputBuilder() {
                                return ${outputType:T}.builder();
                            }

                            @Override
                            public ${sdkSchema:T} schema() {
                                return SCHEMA;
                            }

                            @Override
                            public ${sdkSchema:T} inputSchema() {
                                return ${inputType:T}.SCHEMA;
                            }

                            @Override
                            public boolean streamingInput() {
                                return ${streamingInput:L};
                            }

                            ${inputEventStreamSection:C|}

                            @Override
                            public ${sdkSchema:T} outputSchema() {
                                return ${outputType:T}.SCHEMA;
                            }

                            @Override
                            public boolean streamingOutput() {
                                return ${streamingOutput:L};
                            }

                            @Override
                            public ${typeRegistry:T} typeRegistry() {
                                return typeRegistry;
                            }
                        }
                        """
                );
                writer.popState();
            });
    }

    private boolean hasStream(Model model, Shape shape) {
        return shape.members()
            .stream()
            .anyMatch(member -> member.getMemberTrait(model, StreamingTrait.class).isPresent());
    }


    private record TypeRegistryGenerator(
        JavaWriter writer,
        OperationShape shape,
        SymbolProvider symbolProvider,
        Model model,
        ServiceShape service
    ) implements Runnable {
        @Override
        public void run() {
            writer.write("private final ${typeRegistry:T} typeRegistry = ${typeRegistry:T}.builder()");
            writer.indent();
            writer.write(".putType(${inputType:T}.ID, ${inputType:T}.class, ${inputType:T}::builder)");
            writer.write(".putType(${outputType:T}.ID, ${outputType:T}.class, ${outputType:T}::builder)");
            for (var errorId : shape.getErrors(service)) {
                var errorShape = model.expectShape(errorId);
                writer.write(".putType($1T.ID, $1T.class, $1T::builder)", symbolProvider.toSymbol(errorShape));
            }
            writer.writeWithNoFormatting(".build();");
            writer.dedent();
        }
    }

    private record OperationTypeGenerator(
        JavaWriter writer, OperationShape shape, SymbolProvider symbolProvider,
        Model model, CodeGenerationContext context
    ) implements Runnable {
        @Override
        public void run() {
            var inputShape = model.expectShape(shape.getInputShape());
            var input = symbolProvider.toSymbol(inputShape);
            var outputShape = model.expectShape(shape.getOutputShape());
            var output = symbolProvider.toSymbol(outputShape);
            EventStreamIndex.of(model).getInputInfo(shape).ifPresentOrElse(info -> {
                writer.writeInline(
                    "$1T<$2T, $3T, $4T>",
                    InputEventStreamingSdkOperation.class,
                    input,
                    output,
                    symbolProvider.toSymbol(info.getEventStreamTarget())
                );
            }, () -> {
                writer.writeInline("$1T<$2T, $3T>", ApiOperation.class, input, output);
            });
        }
    }

    private record InputEventStreamGenerator(
        JavaWriter writer, OperationShape shape, SymbolProvider symbolProvider,
        Model model, ServiceShape service
    ) implements Runnable {
        @Override
        public void run() {
            EventStreamIndex.of(model).getInputInfo(shape).ifPresent(info -> {
                writer.write("""
                    @Override
                    public $1T<$2T> inputEventBuilder() {
                        return $2T.builder();
                    }

                    @Override
                    public $3T inputEventSchema() {
                        return $2T.SCHEMA;
                    }
                    """, ShapeBuilder.class, symbolProvider.toSymbol(info.getEventStreamTarget()), Schema.class);
            });
        }
    }
}
