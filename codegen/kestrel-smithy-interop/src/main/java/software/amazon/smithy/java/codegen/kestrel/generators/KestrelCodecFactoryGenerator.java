/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.kestrel.generators;

import static software.amazon.smithy.java.codegen.kestrel.InteropSymbolProperties.SMITHY_SYMBOL;
import static software.amazon.smithy.kestrel.codegen.CommonSymbols.imp;

import java.util.List;
import java.util.function.Consumer;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.SymbolReference;
import software.amazon.smithy.java.codegen.sections.ClassSection;
import software.amazon.smithy.kestrel.codegen.CommonSymbols;
import software.amazon.smithy.kestrel.codegen.JavaWriter;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;

public class KestrelCodecFactoryGenerator implements Consumer<JavaWriter> {

    private static final SymbolReference CODEC_FACTORY = imp(
        "software.amazon.smithy.java.kestrel.codec",
        "KestrelCodecFactory"
    );
    private static final SymbolReference KESTREL_CODEC = imp(
        "software.amazon.smithy.java.kestrel.codec",
        "KestrelCodec"
    );
    private static final SymbolReference UNKNOWN_OPERATION_EXCEPTION = imp(
        "software.amazon.smithy.java.server.exceptions",
        "UnknownOperationException"
    );

    private static final SymbolReference BYTE_BUFFER = imp(
        "java.nio",
        "ByteBuffer"
    );

    private final ServiceShape service;
    private final Model model;
    private final SymbolProvider symbolProvider;
    private final String className;

    public KestrelCodecFactoryGenerator(ServiceShape service, Model model, SymbolProvider symbolProvider) {
        this.service = service;
        this.model = model;
        this.symbolProvider = symbolProvider;
        this.className = service.getId().getName() + "KestrelCodecFactory";
    }

    public String className() {
        return className;
    }

    @Override
    public void accept(JavaWriter writer) {

        TopDownIndex index = TopDownIndex.of(model);
        List<OperationInfo> operationsInfo = index.getContainedOperations(service)
            .stream()
            .map(o -> {
                var inputSymbol = symbolProvider.toSymbol(model.expectShape(o.getInputShape()));
                var outputSymbol = symbolProvider
                    .toSymbol(model.expectShape(o.getOutputShape()));
                var smithyInputSymbol = inputSymbol.expectProperty(SMITHY_SYMBOL);
                var smithyOutputSymbol = outputSymbol.expectProperty(SMITHY_SYMBOL);
                return new OperationInfo(
                    symbolProvider.toSymbol(o),
                    o,
                    inputSymbol,
                    outputSymbol,
                    smithyInputSymbol,
                    smithyOutputSymbol
                );
            })
            .toList();
        List<Symbol> operations = operationsInfo.stream().map(OperationInfo::symbol).toList();
        writer.pushState(new ClassSection(service));
        writer.putContext("codecFactoryImpl", className);
        writer.putContext("codecFactory", CODEC_FACTORY);
        writer.putContext("kestrelCodec", KESTREL_CODEC);
        writer.putContext("serviceId", service.getId());
        writer.putContext("getCodec", new GetCodecGenerator(writer, service, operations));
        writer.putContext("codecs", new CodecGenerator(service, operationsInfo));
        writer.putContext("unknownOperationException", UNKNOWN_OPERATION_EXCEPTION);
        writer.putContext("kestrelDeser", CommonSymbols.KestrelDeserializer);
        writer.putContext("kestrelSer", CommonSymbols.KestrelSerializer);
        var template = """
            public final class ${codecFactoryImpl:L} implements ${codecFactory:T} {
                  @Override
                  public String serviceName() {
                      return ${serviceId:S};
                  }

                  ${getCodec:C|}

                  ${codecs:C|}

            }
            """;
        writer.write(template);
        writer.popState();
    }

    private record GetCodecGenerator(
        JavaWriter writer, ServiceShape shape,
        List<Symbol> operations
    ) implements Runnable {

        @Override
        public void run() {
            writer.write(
                """
                    @Override
                    public ${kestrelCodec:T} getCodec(String operationName) {
                        return switch (operationName) {
                            ${C|}
                        };
                    }
                    """,
                writer.consumer(w -> {
                    for (Symbol operation : operations) {
                        w.write("case \"$L\" -> $L.INSTANCE;", operation.getName(), codecName(operation.getName()));
                    }
                    w.write("default -> throw new ${unknownOperationException:T}(operationName);");
                })
            );
        }
    }

    private static String codecName(String operationName) {
        return operationName + "KestrelCodec";
    }

    private record CodecGenerator(ServiceShape service, List<OperationInfo> operations) implements
        Consumer<JavaWriter> {

        @Override
        public void accept(JavaWriter writer) {
            for (OperationInfo operation : operations) {
                writer.pushState();
                writer.putContext("codecClass", codecName(operation.operationShape.getId().getName()));
                writer.putContext("input", operation.smithyInputSymbol);
                writer.putContext("output", operation.smithyOutputSymbol);
                writer.putContext("kestrelInput", operation.kestrelInputSymbol);
                writer.putContext("kestrelOutput", operation.kestrelOutputSymbol);
                writer.putContext("byteBuf", BYTE_BUFFER);
                var template = """
                    private static final class ${codecClass:L} extends ${kestrelCodec:T}<${input:T}, ${output:T}, ${kestrelInput:T}, ${kestrelOutput:T}> {
                        private static final ${codecClass:L} INSTANCE = new ${codecClass:L}();

                        private ${codecClass:L}() {
                        }

                        @Override
                        public byte[] encode(${output:T} value) {
                            ${kestrelOutput:T} converted = new ${kestrelOutput:T}();
                            converted.convertFrom(value);
                            return serialize(converted);
                        }

                        @Override
                        public ${input:T} decode(${byteBuf:T} buf) {
                            return deserialize(buf, new ${kestrelInput:T}());
                        }
                    }
                    """;
                writer.write(template);
                writer.popState();
            }

        }
    }

    private record OperationInfo(
        Symbol symbol, OperationShape operationShape, Symbol kestrelInputSymbol, Symbol kestrelOutputSymbol,
        Symbol smithyInputSymbol, Symbol smithyOutputSymbol
    ) {
    }
}
