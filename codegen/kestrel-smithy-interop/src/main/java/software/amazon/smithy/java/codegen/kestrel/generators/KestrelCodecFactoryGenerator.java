/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.kestrel.generators;

import static software.amazon.smithy.java.codegen.kestrel.InteropSymbolProperties.SMITHY_SYMBOL;
import static software.amazon.smithy.kestrel.codegen.CommonSymbols.imp;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Consumer;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.SymbolReference;
import software.amazon.smithy.java.codegen.sections.ClassSection;
import software.amazon.smithy.java.kestrel.codec.KestrelCodec;
import software.amazon.smithy.java.kestrel.codec.KestrelCodecFactory;
import software.amazon.smithy.java.runtime.core.schema.ModeledApiException;
import software.amazon.smithy.kestrel.codegen.CommonSymbols;
import software.amazon.smithy.kestrel.codegen.JavaWriter;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;

public class KestrelCodecFactoryGenerator implements Consumer<JavaWriter> {

    private static final SymbolReference CODEC_FACTORY = imp(KestrelCodecFactory.class);
    private static final SymbolReference KESTREL_CODEC = imp(KestrelCodec.class);

    private static final SymbolReference UNKNOWN_OPERATION_EXCEPTION = imp(
        "software.amazon.smithy.java.server.exceptions",
        "UnknownOperationException"
    );

    private static final SymbolReference BYTE_BUFFER = imp(ByteBuffer.class);

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
        writer.putContext("codecs", new CodecGenerator(service, model, symbolProvider, operationsInfo));
        writer.putContext("unknownOperationException", UNKNOWN_OPERATION_EXCEPTION);
        writer.putContext("kestrelDeser", CommonSymbols.KestrelDeserializer);
        writer.putContext("kestrelSer", CommonSymbols.KestrelSerializer);
        writer.putContext("kestrelObj", CommonSymbols.KestrelObject);
        writer.putContext("kestrelStruct", CommonSymbols.imp(KestrelCodec.class));
        writer.putContext("modeledException", imp(ModeledApiException.class));
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

    private record CodecGenerator(
        ServiceShape service, Model model, SymbolProvider symbolProvider, List<OperationInfo> operations
    ) implements
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
                writer.putContext("exceptionEncoder", new ExceptionEncoder(service, model, symbolProvider, operation));
                var template = """
                    private static final class ${codecClass:L} extends ${kestrelCodec:T}<${input:T}, ${output:T}, ${kestrelInput:T}, ${kestrelOutput:T}> {
                        private static final ${codecClass:L} INSTANCE = new ${codecClass:L}();

                        private ${codecClass:L}() {
                        }

                        @Override
                        public byte[] encode(${output:T} value) {
                            return serialize(${kestrelOutput:T}.convertFrom(value));
                        }

                        @Override
                        public ${input:T} decode(${byteBuf:T} buf) {
                            return deserialize(buf, new ${kestrelInput:T}());
                        }

                        ${exceptionEncoder:C|}
                    }
                    """;
                writer.write(template);
                writer.popState();
            }

        }

        private record ExceptionEncoder(
            ServiceShape service, Model model, SymbolProvider symbolProvider, OperationInfo operationInfo
        ) implements Consumer<JavaWriter> {

            @Override
            public void accept(JavaWriter writer) {
                var errors = operationInfo.operationShape.getErrors()
                    .stream()
                    .map(model::expectShape)
                    .map(symbolProvider::toSymbol)
                    .toList();
                writer.pushState();
                writer.putContext("errorMatcher", writer.consumer(w -> {
                    for (var error : errors) {
                        var smithySymbol = error.expectProperty(SMITHY_SYMBOL);
                        writer.putContext("kestrelError", error);
                        writer.putContext("smithyError", smithySymbol);
                        w.write("""
                            if (exception instanceof ${smithyError:T} e) {
                                return ${kestrelError:T}.convertFrom(e);
                            }
                            """);
                    }
                }));
                var template = """
                    @Override
                    public ${kestrelObj:T} convertException(Throwable exception) {
                        ${errorMatcher:C|}
                        return createSynthetic(exception);
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
