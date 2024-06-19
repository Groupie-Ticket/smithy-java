/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.kestrel.generators;

import static software.amazon.smithy.java.codegen.kestrel.InteropSymbolProperties.SMITHY_SYMBOL;
import static software.amazon.smithy.kestrel.codegen.CommonSymbols.FLOW_PUBLISHER;
import static software.amazon.smithy.kestrel.codegen.CommonSymbols.imp;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.SymbolReference;
import software.amazon.smithy.java.codegen.sections.ClassSection;
import software.amazon.smithy.java.kestrel.KestrelObject;
import software.amazon.smithy.java.kestrel.codec.KestrelCodec;
import software.amazon.smithy.java.kestrel.codec.KestrelCodecFactory;
import software.amazon.smithy.java.runtime.core.schema.ModeledApiException;
import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;
import software.amazon.smithy.kestrel.codegen.CommonSymbols;
import software.amazon.smithy.kestrel.codegen.JavaWriter;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StreamingTrait;

public class KestrelCodecFactoryGenerator implements Consumer<JavaWriter> {

    private static final SymbolReference CODEC_FACTORY = imp(KestrelCodecFactory.class);
    private static final SymbolReference KESTREL_CODEC = imp(KestrelCodec.class);

    private static final SymbolReference UNKNOWN_OPERATION_EXCEPTION = imp(
        "software.amazon.smithy.java.server.exceptions",
        "UnknownOperationException"
    );

    private static final SymbolReference BYTE_BUFFER = imp(ByteBuffer.class);
    private static final SymbolReference KESTREL_OBJECT = imp(KestrelObject.class);
    private static final SymbolReference SERIALIZABLE_STRUCT = imp(SerializableStruct.class);

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
        writer.putContext("codecs", new CodecGenerator(operationsInfo));
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

    private boolean isEventStreaming(MemberShape memberShape) {
        var shape = model.expectShape(memberShape.getTarget());
        return shape.isUnionShape() && shape.hasTrait(StreamingTrait.class);
    }

    private static String codecName(String operationName) {
        return operationName + "KestrelCodec";
    }

    private class CodecGenerator implements Consumer<JavaWriter> {
        private final List<OperationInfo> operations;

        private CodecGenerator(List<OperationInfo> operations) {
            this.operations = operations;
        }

        private Optional<MemberShape> getEventStreamingMember(ShapeId shapeId) {
            return model.expectShape(shapeId)
                .getAllMembers()
                .values()
                .stream()
                .filter(member -> isEventStreaming(member))
                .findFirst();
        }

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
                writer.putContext("kestrelObject", KESTREL_OBJECT);
                writer.putContext("serializableStruct", SERIALIZABLE_STRUCT);
                getEventStreamingMember(operation.operationShape.getInputShape())
                    .ifPresent(streamingMember -> {
                        writer.putContext("deserializeEvent", new InboundEventStreamingGenerator(streamingMember));
                    });
                getEventStreamingMember(operation.operationShape.getOutputShape())
                    .ifPresent(streamingMember -> {
                        writer.putContext("serializeEvent", new OutboundEventStreamingGenerator(streamingMember));
                    });
                var template = """
                    private static final class ${codecClass:L} extends ${kestrelCodec:T} {
                        private static final ${codecClass:L} INSTANCE = new ${codecClass:L}();

                        private ${codecClass:L}() {
                        }

                        @Override
                        public byte[] encode(${serializableStruct:T} value) {
                            return serialize(${kestrelOutput:T}.convertFrom((${output:T}) value));
                        }

                        ${exceptionEncoder:C|}

                        ${?serializeEvent}
                        ${serializeEvent:C|}${/serializeEvent}

                        ${^deserializeEvent}
                        @Override
                        public ${input:T} decode(${byteBuf:T} buf) {
                            return deserialize(buf, new ${kestrelInput:T}());
                        }

                        ${/deserializeEvent}
                        ${?deserializeEvent}

                        ${deserializeEvent:C|}${/deserializeEvent}
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

    private class InboundEventStreamingGenerator implements Consumer<JavaWriter> {
        private final MemberShape memberShape;

        private InboundEventStreamingGenerator(MemberShape memberShape) {
            this.memberShape = memberShape;
        }

        @Override
        public void accept(JavaWriter writer) {
            writer.pushState();
            writer.putContext("publisher", FLOW_PUBLISHER);
            var decodeTemplate = """
                @Override
                public ${input:T} decodeInitialRequest(${byteBuf:T} buffer, ${publisher:T}<?> publisher) {
                    return deserializeStreaming(buffer, new ${kestrelInput:T}(), publisher);
                }""";
            writer.write(decodeTemplate);
            writer.popState();

            writer.pushState();
            var target = model.expectShape(memberShape.getTarget());
            var kestrelType = symbolProvider.toSymbol(target);
            var smithySymbol = kestrelType.expectProperty(SMITHY_SYMBOL);
            writer.putContext("smithySymbol", smithySymbol);
            writer.putContext("kestrelSymbol", kestrelType);
            writer.write("""

                @Override
                public ${smithySymbol:T} deserializeEvent(${byteBuf:T} buf) {
                    return deserialize(buf, new ${kestrelSymbol:T}());
                }""");
            writer.popState();
        }
    }

    private class OutboundEventStreamingGenerator implements Consumer<JavaWriter> {
        private final MemberShape memberShape;

        private OutboundEventStreamingGenerator(MemberShape memberShape) {
            this.memberShape = memberShape;
        }

        @Override
        public void accept(JavaWriter writer) {
            writer.pushState();
            writer.putContext("publisher", FLOW_PUBLISHER);
            writer.putContext("serializableStruct", SERIALIZABLE_STRUCT);
            writer.putContext("member", memberShape.getMemberName());
            var encodeTemplate = """
                @Override
                public ${publisher:T}<? extends ${serializableStruct:T}> getStreamMember(${serializableStruct:T} output) {
                    return ((${output:T}) output).${member:L}();
                }""";
            writer.write(encodeTemplate);
            writer.popState();

            writer.pushState();
            var target = model.expectShape(memberShape.getTarget());
            var kestrelType = symbolProvider.toSymbol(target);
            var smithySymbol = kestrelType.expectProperty(SMITHY_SYMBOL);
            writer.putContext("smithySymbol", smithySymbol);
            writer.putContext("kestrelSymbol", kestrelType);
            writer.write("""

                @Override
                public byte[] encodeEvent(${serializableStruct:T} event) {
                    return serialize(${kestrelSymbol:T}.convertFrom((${smithySymbol:T}) event));
                }""");
            writer.popState();
        }
    }

    private record OperationInfo(
        Symbol symbol, OperationShape operationShape, Symbol kestrelInputSymbol, Symbol kestrelOutputSymbol,
        Symbol smithyInputSymbol, Symbol smithyOutputSymbol
    ) {
    }
}
