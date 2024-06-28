/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.sparrowhawk.generators;

import static java.util.function.Function.identity;
import static software.amazon.smithy.java.codegen.sparrowhawk.InteropSymbolProperties.SMITHY_MEMBER;
import static software.amazon.smithy.java.codegen.sparrowhawk.InteropSymbolProperties.SMITHY_SYMBOL;
import static software.amazon.smithy.sparrowhawk.codegen.CommonSymbols.FLOW_PUBLISHER;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.SparseTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.sparrowhawk.codegen.CodeSections.EndClassSection;
import software.amazon.smithy.sparrowhawk.codegen.CommonSymbols;
import software.amazon.smithy.sparrowhawk.codegen.JavaWriter;
import software.amazon.smithy.sparrowhawk.codegen.StructureGenerator;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.StringUtils;

public class ConverterMethodsGenerator implements CodeInterceptor<EndClassSection, JavaWriter> {

    @Override
    public Class<EndClassSection> sectionType() {
        return EndClassSection.class;
    }

    @Override
    public void write(JavaWriter writer, String s, EndClassSection section) {
        var generator = section.generator();
        var model = generator.getModel();
        var smithySymbol = generator.getSymbol().expectProperty(SMITHY_SYMBOL);
        var template = """

            @Override
            public Class<${smithySymbol:T}> getConvertedType() {
                return ${smithySymbol:T}.class;
            }

            @Override
            public ${shapeId:T} getConvertedId() {
                return ${smithySymbol:T}.ID;
            }

            ${convertTo:C|}

            public static ${sparrowhawkSymbol:T} convertFrom(${smithySymbol:T} o) {
                ${convertFrom:C|}
            }

            """;

        writer.pushState();
        var streamingMember = generator.getShape()
            .getAllMembers()
            .values()
            .stream()
            .filter(member -> isEventStream(model, member))
            .findFirst();
        writer.putContext("smithySymbol", smithySymbol);
        writer.putContext("sparrowhawkSymbol", generator.getSymbol());
        writer.putContext("hashMap", CommonSymbols.imp(HashMap.class));
        writer.putContext("arrayList", CommonSymbols.imp(ArrayList.class));
        writer.putContext("list", CommonSymbols.imp(List.class));
        writer.putContext("map", CommonSymbols.imp(Map.class));
        writer.putContext("byteBuffer", CommonSymbols.imp(ByteBuffer.class));
        writer.putContext("shapeId", CommonSymbols.imp(ShapeId.class));
        writer.putContext("convertTo", new ConvertToGenerator(generator, writer, smithySymbol, model, streamingMember));
        writer.putContext("convertFrom", new ConvertFromGenerator(generator, writer, smithySymbol, model));
        writer.write(template);
        writer.popState();

    }

    private static boolean isEventStream(Model model, MemberShape memberShape) {
        var target = model.expectShape(memberShape.getTarget());
        return target.isUnionShape() && target.hasTrait(StreamingTrait.class);
    }

    private record ConvertToGenerator(
        StructureGenerator generator, JavaWriter writer,
        Symbol smithySymbol, Model model, Optional<MemberShape> eventStreamMember
    ) implements Runnable {
        @Override
        public void run() {
            eventStreamMember.ifPresent(member -> {
                writer.putContext("publisher", FLOW_PUBLISHER);
            });
            writer.openBlock(
                "@Override\npublic ${smithySymbol:T} convertTo(${?publisher}${publisher:T}<?> publisher${/publisher}) {"
            );
            writer.write("var builder = ${smithySymbol:T}.builder();");
            Stream.of(
                generator.getAllVarintMembers(),
                generator.getAllFourByteMembers(),
                generator.getAllEightByteMembers(),
                generator.getAllListMembers()
            )
                .flatMap(identity())
                .forEach(member -> {
                    writer.pushState();
                    writer.putContext("optional", generator.isOptional(member));
                    String methodName = generator.methodNameForField(member);
                    writer.putContext("sparrowhawkHas", "has" + methodName);
                    writer.putContext("sparrowhawkGetter", "get" + methodName);
                    Symbol sparrowhawkMemberSymbol = generator.getSymbolProvider().toSymbol(member);
                    Symbol smithyMemberSymbol = generator.getSymbolProvider()
                        .toSymbol(member)
                        .expectProperty(SMITHY_SYMBOL);
                    writer.putContext(
                        "builderMethod",
                        sparrowhawkMemberSymbol.expectProperty(SMITHY_MEMBER)
                    );
                    writer.putContext("smithyMemberSymbol", smithyMemberSymbol);
                    Shape memberShape = model.expectShape(member.getTarget());
                    String sparrowhawkValue = "${sparrowhawkGetter:L}()";
                    boolean requiresTransformation = false;
                    if (memberShape.isStructureShape() || memberShape.isUnionShape()) {
                        sparrowhawkValue = "${sparrowhawkGetter:L}().convertTo()";
                    } else if (memberShape.isListShape()) {
                        Shape target = model.expectShape(
                            memberShape.asListShape().orElseThrow().getMember().getTarget()
                        );
                        if (target.isStructureShape() || target.isUnionShape()) {
                            requiresTransformation = true;
                            writer.putContext("transformer", (Consumer<JavaWriter>) (w) -> {
                                var targetSmithySymbol = generator.getSymbolProvider()
                                    .toSymbol(target)
                                    .expectProperty(SMITHY_SYMBOL);
                                w.pushState();
                                w.putContext("isSparse", memberShape.hasTrait(SparseTrait.class));
                                w.putContext("targetSmithySymbol", targetSmithySymbol);
                                w.write(
                                    """
                                        var ${convertedVariable:L} = new ${arrayList:T}<${targetSmithySymbol:T}>(${sparrowhawkGetter:L}().size());
                                        for(var value : ${sparrowhawkGetter:L}()) {
                                            ${convertedVariable:L}.add(${?isSparse} value == null ? null : ${/isSparse}value.convertTo());
                                        }
                                        """
                                );
                                w.popState();
                            });
                        }
                    } else if (memberShape.isMapShape()) {
                        Shape target = model.expectShape(memberShape.asMapShape().orElseThrow().getValue().getTarget());
                        if (target.isStructureShape() || target.isUnionShape()) {
                            requiresTransformation = true;
                            writer.putContext("transformer", (Consumer<JavaWriter>) (w) -> {
                                var targetSmithySymbol = generator.getSymbolProvider()
                                    .toSymbol(target)
                                    .expectProperty(SMITHY_SYMBOL);
                                w.pushState();
                                w.putContext("isSparse", memberShape.hasTrait(SparseTrait.class));
                                w.putContext("targetSmithySymbol", targetSmithySymbol);
                                w.write(
                                    """
                                        var ${convertedVariable:L} = new ${hashMap:T}<String, ${targetSmithySymbol:T}>(${sparrowhawkGetter:L}().size());
                                        for(var e : ${sparrowhawkGetter:L}().entrySet()) {
                                            var value = e.getValue();
                                            ${convertedVariable:L}.put(e.getKey(), ${?isSparse} value == null ? null : ${/isSparse}value.convertTo());
                                        }
                                        """
                                );
                                w.popState();
                            });
                        }
                    } else if (memberShape.isEnumShape() || memberShape.isIntEnumShape()) {
                        sparrowhawkValue = "${smithyMemberSymbol:T}.builder().value(${sparrowhawkGetter:L}()).build()";
                    } else if (memberShape.isBlobShape()) {
                        requiresTransformation = true;
                        writer.putContext("transformer", (Consumer<JavaWriter>) (w) -> {
                            w.write("""
                                 var copy${sparrowhawkGetter:L} = ${sparrowhawkGetter:L}().slice();
                                 byte[] ${convertedVariable:L} = new byte[copy${sparrowhawkGetter:L}.remaining()];
                                 copy${sparrowhawkGetter:L}.get(${convertedVariable:L});
                                """);
                        });
                    }
                    writer.putContext("transformation", requiresTransformation);
                    if (requiresTransformation) {
                        writer.putContext(
                            "convertedVariable",
                            "converted" + StringUtils.capitalize(member.getMemberName())
                        );
                        sparrowhawkValue = "${convertedVariable:L}";
                    }
                    writer.write("""
                        ${?optional}if (${sparrowhawkHas:L}()) {
                            ${/optional}${?transformation}${transformer:C|}
                            ${/transformation}builder.${builderMethod:L}(%s);${?optional}
                        }${/optional}
                        """.formatted(sparrowhawkValue));
                    writer.popState();
                });

            eventStreamMember.ifPresent(member -> {
                writer.putContext("streamingMember", member.getMemberName());
                Symbol sparrowhawkMemberSymbol = generator.getSymbolProvider().toSymbol(member);
                writer.putContext(
                    "builderMethod",
                    sparrowhawkMemberSymbol.expectProperty(SMITHY_MEMBER)
                );
                var smithyEventType = generator.getSymbolProvider()
                    .toSymbol(model.expectShape(member.getTarget()))
                    .expectProperty(SMITHY_SYMBOL);
                writer.putContext("streamType", smithyEventType);
                writer.write("""
                    builder.${builderMethod:L}((${publisher:T}<${streamType:T}>) publisher);""");
            });

            writer.write("return builder.build();");
            writer.closeBlock("}");
        }
    }

    private record ConvertFromGenerator(
        StructureGenerator generator, JavaWriter writer,
        Symbol smithySymbol, Model model
    ) implements Runnable {
        @Override
        public void run() {
            writer.write("${sparrowhawkSymbol:T} k = new ${sparrowhawkSymbol:T}();");
            boolean isError = generator.getShape().hasTrait(ErrorTrait.class);
            Stream.of(
                generator.getAllVarintMembers(),
                generator.getAllFourByteMembers(),
                generator.getAllEightByteMembers(),
                generator.getAllListMembers()
            )
                .flatMap(identity())
                .forEach(member -> {
                    Shape memberShape = model.expectShape(member.getTarget());
                    writer.pushState();
                    writer.putContext("optional", generator.isOptional(member));
                    String methodName = generator.methodNameForField(member);
                    writer.putContext("sparrowhawkSetter", "set" + methodName);
                    String smithyGetter = generator.getSymbolProvider().toSymbol(member).expectProperty(SMITHY_MEMBER);
                    if (isError && smithyGetter.equals("message")) {
                        smithyGetter = "getMessage";
                    }

                    writer.putContext("smithyGetter", smithyGetter);
                    writer.putContext("sparrowhawkMemberSymbol", generator.getSymbolProvider().toSymbol(member));
                    String sparrowhawkValue = "o.${smithyGetter:L}()";
                    boolean requiresTransformation = false;
                    if (memberShape.isStructureShape() || memberShape.isUnionShape()) {
                        sparrowhawkValue = "${sparrowhawkMemberSymbol:T}.convertFrom(o.${smithyGetter:L}())";
                    } else if (memberShape.isMapShape()) {
                        Shape target = model.expectShape(memberShape.asMapShape().orElseThrow().getValue().getTarget());
                        if (target.isStructureShape() || target.isUnionShape()) {
                            requiresTransformation = true;
                            writer.putContext("transformer", (Consumer<JavaWriter>) (w) -> {
                                var sparrowhawkSymbol = generator.getSymbolProvider().toSymbol(target);
                                w.pushState();
                                w.putContext("isSparse", memberShape.hasTrait(SparseTrait.class));
                                w.putContext("sparrowhawkSymbol", sparrowhawkSymbol);
                                w.write(
                                    """
                                        var ${convertedVariable:L} = new ${hashMap:T}<String, ${sparrowhawkSymbol:T}>(o.${smithyGetter:L}().size());
                                        for(var e : o.${smithyGetter:L}().entrySet()) {
                                            var value = e.getValue();
                                            ${convertedVariable:L}.put(e.getKey(), ${?isSparse} value == null ? null : ${/isSparse}${sparrowhawkSymbol:T}.convertFrom(value));
                                        }
                                        """
                                );
                                w.popState();
                            });
                        }
                    } else if (memberShape.isListShape()) {
                        Shape target = model.expectShape(
                            memberShape.asListShape().orElseThrow().getMember().getTarget()
                        );
                        if (target.isStructureShape() || target.isUnionShape()) {
                            requiresTransformation = true;
                            writer.putContext("transformer", (Consumer<JavaWriter>) (w) -> {
                                var sparrowhawkSymbol = generator.getSymbolProvider().toSymbol(target);
                                w.pushState();
                                w.putContext("isSparse", memberShape.hasTrait(SparseTrait.class));
                                w.putContext("sparrowhawkSymbol", sparrowhawkSymbol);
                                w.write(
                                    """
                                        var ${convertedVariable:L} = new ${arrayList:T}<${sparrowhawkSymbol:T}>(o.${smithyGetter:L}().size());
                                        for(var value : o.${smithyGetter:L}()) {
                                            ${convertedVariable:L}.add(${?isSparse} value == null ? null : ${/isSparse}${sparrowhawkSymbol:T}.convertFrom(value));
                                        }
                                        """
                                );
                                w.popState();
                            });
                        }

                    } else if (memberShape.isEnumShape() || memberShape.isIntEnumShape()) {
                        sparrowhawkValue = "o.${smithyGetter:L}().value()";
                    } else if (memberShape.isBlobShape()) {
                        sparrowhawkValue = "${byteBuffer:T}.wrap(o.${smithyGetter:L}())";
                    }

                    writer.putContext("transformation", requiresTransformation);
                    if (requiresTransformation) {
                        writer.putContext(
                            "convertedVariable",
                            "converted" + StringUtils.capitalize(member.getMemberName())
                        );
                        sparrowhawkValue = "${convertedVariable:L}";

                    }
                    writer.write("""
                        ${?optional}if (o.${smithyGetter:L}() != null) {
                            ${/optional}
                            ${?transformation}${transformer:C|}
                            ${/transformation}k.${sparrowhawkSetter:L}(%s);${?optional}
                        }${/optional}
                        """.formatted(sparrowhawkValue));
                    writer.popState();
                });
            writer.write("return k;");
        }
    }
}
