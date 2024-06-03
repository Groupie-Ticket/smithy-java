/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.server.generators;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.directed.GenerateServiceDirective;
import software.amazon.smithy.java.codegen.CodeGenerationContext;
import software.amazon.smithy.java.codegen.JavaCodegenSettings;
import software.amazon.smithy.java.codegen.generators.IdStringGenerator;
import software.amazon.smithy.java.codegen.generators.SchemaGenerator;
import software.amazon.smithy.java.codegen.sections.ClassSection;
import software.amazon.smithy.java.codegen.server.ServerSymbolProperties;
import software.amazon.smithy.java.codegen.writer.JavaWriter;
import software.amazon.smithy.java.runtime.core.schema.SdkSchema;
import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;
import software.amazon.smithy.java.server.Operation;
import software.amazon.smithy.java.server.Service;
import software.amazon.smithy.java.server.exceptions.UnknownOperationException;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;

public class ServiceGenerator implements
    Consumer<GenerateServiceDirective<CodeGenerationContext, JavaCodegenSettings>> {

    @Override
    public void accept(
        GenerateServiceDirective<CodeGenerationContext, JavaCodegenSettings> directive
    ) {
        ServiceShape shape = directive.shape();
        TopDownIndex index = TopDownIndex.of(directive.model());
        List<OperationInfo> operationsInfo = index.getContainedOperations(shape)
            .stream()
            .map(o -> {
                var inputSymbol = directive.symbolProvider().toSymbol(directive.model().expectShape(o.getInputShape()));
                var outputSymbol = directive.symbolProvider()
                    .toSymbol(directive.model().expectShape(o.getOutputShape()));
                return new OperationInfo(directive.symbolProvider().toSymbol(o), o, inputSymbol, outputSymbol);
            })
            .toList();
        List<Symbol> operations = operationsInfo.stream().map(OperationInfo::symbol).toList();
        directive.context().writerDelegator().useShapeWriter(shape, writer -> {
            writer.pushState(new ClassSection(shape));
            var template = """
                public final class ${service:T} implements ${serviceType:T} {
                    ${id:C|}

                    ${properties:C|}

                    ${constructor:C|}

                    ${builder:C|}

                    @Override
                    public <I, O> ${operationHolder:T}<I, O> getOperation(String operationName) {
                        ${getOperation:C|}
                    }
                }
                """;
            writer.putContext("operationHolder", Operation.class);
            writer.putContext("serviceType", Service.class);
            writer.putContext("service", directive.symbol());
            writer.putContext("id", new IdStringGenerator(writer, shape));
            writer.putContext(
                "properties",
                new PropertyGenerator(writer, shape, directive.symbolProvider(), operationsInfo, false)
            );
            writer.putContext(
                "constructor",
                new ConstructorGenerator(writer, shape, directive.symbolProvider(), operations)
            );
            writer.putContext(
                "builder",
                new BuilderGenerator(writer, shape, directive.symbolProvider(), operationsInfo)
            );
            writer.putContext("serializableStruct", SerializableStruct.class);
            writer.putContext(
                "schema",
                new SchemaGenerator(writer, shape, directive.symbolProvider(), directive.model(), directive.context())
            );
            writer.putContext("sdkSchema", SdkSchema.class);
            writer.write(
                """
                    public final class ${service:T} implements ${serviceType:T} {
                        ${id:C|}

                        private static final ${sdkSchema:T} SCHEMA = ${schema:C}

                        ${properties:C|}

                        ${constructor:C|}

                        ${builder:C|}

                        @Override
                        public <I extends ${serializableStruct:T}, O extends ${serializableStruct:T}> ${operationHolder:T}<I, O> getOperation(String operationName) {
                            ${C|}
                        }

                        @Override
                        public ${sdkSchema:T} getSchema() {
                            return SCHEMA;
                        }
                    }
                    """,
                new GetOperationGenerator(writer, shape, directive.symbolProvider(), operations)
            );
            writer.popState();
        });


    }

    private record PropertyGenerator(
        JavaWriter writer, ServiceShape serviceShape, SymbolProvider symbolProvider,
        List<OperationInfo> operations, boolean notFinal
    ) implements Runnable {

        @Override
        public void run() {
            for (var operationInfo : operations) {
                var operation = operationInfo.symbol();
                var operationName = operation.getProperty(ServerSymbolProperties.OPERATION_FIELD_NAME);
                writer.pushState();
                writer.putContext("input", operationInfo.inputSymbol());
                writer.putContext("output", operationInfo.outputSymbol());
                writer.putContext("notFinal", notFinal);
                writer.write(
                    "private${^notFinal} final${/notFinal} $T<${input:T}, ${output:T}> $L;",
                    Operation.class,
                    operationName
                );
                writer.popState();
            }
        }
    }

    private record ConstructorGenerator(
        JavaWriter writer, ServiceShape serviceShape, SymbolProvider symbolProvider,
        List<Symbol> operations
    ) implements Runnable {
        @Override
        public void run() {
            writer.write(
                """
                    private ${service:T}(Builder builder) {
                        ${C|}
                    }
                    """,
                writer.consumer(w -> {
                    for (Symbol operation : operations) {
                        var operationName = operation.getProperty(ServerSymbolProperties.OPERATION_FIELD_NAME);
                        w.write("this.$1L = builder.$1L;", operationName);
                    }
                })
            );
        }
    }

    private record BuilderGenerator(
        JavaWriter writer, ServiceShape serviceShape, SymbolProvider symbolProvider,
        List<OperationInfo> operations
    ) implements Runnable {

        @Override
        public void run() {
            List<String> stages = operations.stream()
                .map(OperationInfo::symbol)
                .map(symbol -> symbol.getName() + "Stage")
                .collect(Collectors.toList());
            stages.add("BuildStage");
            writer.pushState();
            writer.putContext("stages", stages);
            for (int i = 0; i < stages.size() - 1; i++) {
                writer.pushState();
                writer.putContext("curStage", stages.get(i));
                writer.putContext("nextStage", stages.get(i + 1));
                Symbol operation = operations.get(i).symbol();
                Symbol syncOperation = operation.expectProperty(ServerSymbolProperties.STUB_OPERATION);
                Symbol asyncOperation = operation.expectProperty(ServerSymbolProperties.ASYNC_STUB_OPERATION);
                writer.putContext("operation", operation);
                writer.write("""
                    public interface ${curStage:L} {
                        ${nextStage:L} add${operation:T}Operation($T operation);
                        ${nextStage:L} add${operation:T}Operation($T operation);
                    }
                    """, syncOperation, asyncOperation);
                writer.popState();
            }
            writer.write("""
                public interface BuildStage {
                    ${service:T} build();
                }
                """);
            writer.write("""
                public static $L builder() {
                    return new Builder();
                }
                """, stages.get(0));
            writer.write(
                """
                    private final static class Builder implements ${#stages}${value:L}${^key.last}, ${/key.last}${/stages} {

                        ${C|}

                        ${C|}

                        public ${service:T} build() {
                            return new ${service:T}(this);
                        }
                    }
                    """,
                new PropertyGenerator(writer, serviceShape, symbolProvider, operations, true),
                (Runnable) () -> this.generateStages(stages)
            );
            writer.popState();

        }

        private void generateStages(List<String> stages) {
            for (int i = 0; i < operations.size(); i++) {
                Symbol operation = operations.get(i).symbol();
                String operationFieldName = operation.expectProperty(
                    ServerSymbolProperties.OPERATION_FIELD_NAME
                );
                String nextStage = stages.get(i + 1);
                Symbol syncOperation = operation.expectProperty(ServerSymbolProperties.STUB_OPERATION);
                Symbol asyncOperation = operation.expectProperty(ServerSymbolProperties.ASYNC_STUB_OPERATION);
                Symbol sdkOperation = operation.expectProperty(ServerSymbolProperties.SDK_OPERATION);
                writer.pushState();
                writer.putContext("operationFieldName", operationFieldName);
                writer.putContext("nextStage", nextStage);
                writer.putContext("operation", operation);
                writer.putContext("asyncOperation", asyncOperation);
                writer.putContext("syncOperation", syncOperation);
                writer.putContext("sdkOperation", sdkOperation);
                writer.write(
                    """
                        @Override
                        public ${nextStage:L} add${operation:T}Operation(${asyncOperation:T} operation) {
                            this.${operationFieldName:L} = $1T.ofAsync("${operation:T}", operation::${operationFieldName:L}, new ${sdkOperation:T}());
                            return this;
                        }

                        @Override
                        public ${nextStage:L} add${operation:T}Operation(${syncOperation:T} operation) {
                            this.${operationFieldName:L} = $1T.of("${operation:T}", operation::${operationFieldName:L}, new ${sdkOperation:T}());
                            return this;
                        }
                        """,
                    Operation.class
                );
                writer.popState();
            }
        }
    }

    private record GetOperationGenerator(
        JavaWriter writer, ServiceShape serviceShape, SymbolProvider symbolProvider,
        List<Symbol> operations
    ) implements Runnable {

        @Override
        public void run() {
            writer.openBlock("return switch (operationName) {", "};", () -> {
                for (Symbol operation : operations) {
                    writer.write(
                        "case $S -> (Operation<I, O>) $L;",
                        operation.getName(),
                        operation.expectProperty(ServerSymbolProperties.OPERATION_FIELD_NAME)
                    );
                }
                writer.write(
                    "default -> throw new $T(\"Unknown operation name: \" + operationName);",
                    UnknownOperationException.class
                );
            });

        }
    }

    private record OperationInfo(
        Symbol symbol, OperationShape operationShape, Symbol inputSymbol, Symbol outputSymbol
    ) {}
}
