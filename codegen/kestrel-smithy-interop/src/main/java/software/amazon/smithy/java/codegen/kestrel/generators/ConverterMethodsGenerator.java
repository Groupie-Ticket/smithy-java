/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.kestrel.generators;

import static java.util.function.Function.identity;
import static software.amazon.smithy.java.codegen.kestrel.InteropSymbolProperties.SMITHY_SYMBOL;

import java.util.stream.Stream;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.kestrel.codegen.CodeSections.EndClassSection;
import software.amazon.smithy.kestrel.codegen.JavaWriter;
import software.amazon.smithy.kestrel.codegen.StructureGenerator;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.StreamingTrait;
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
        if (generator.getShape().hasTrait(StreamingTrait.class)) {
            return;
        }

        var template = """

            @Override
            public Class<${smithySymbol:T}> getConvertedType() {
                return ${smithySymbol:T}.class;
            }

            @Override
            public ${smithySymbol:T} convertTo() {
                ${convertTo:C|}
            }

            @Override
            public void convertFrom(${smithySymbol:T} o) {
                ${convertFrom:C|}
            }
            """;

        writer.pushState();
        writer.putContext("smithySymbol", smithySymbol);
        writer.putContext("kestrelSymbol", generator.getSymbol());
        writer.putContext("convertTo", new ConvertToGenerator(generator, writer, smithySymbol, model));
        writer.putContext("convertFrom", new ConvertFromGenerator(generator, writer, smithySymbol, model));
        writer.write(template);
        writer.popState();

    }

    private record ConvertToGenerator(
        StructureGenerator generator, JavaWriter writer,
        Symbol smithySymbol, Model model
    ) implements Runnable {

        @Override
        public void run() {
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
                    writer.putContext("kestrelHas", "has" + methodName);
                    writer.putContext("kestrelGetter", "get" + methodName);
                    writer.putContext(
                        "builderMethod",
                        generator.getSymbolProvider().toSymbol(member).expectProperty("smithyMemberName", String.class)
                    );
                    Shape targetShape = model.expectShape(member.getTarget());
                    String kestrelMethod;
                    if (targetShape.isStructureShape() || targetShape.isUnionShape()) {
                        kestrelMethod = "${kestrelGetter:L}().convertTo()";
                    } else {
                        kestrelMethod = "${kestrelGetter:L}()";
                    }
                    writer.write("""
                        ${?optional}if (${kestrelHas:L}()) {
                            ${/optional}builder.${builderMethod:L}(%s);${?optional}
                        }${/optional}
                        """.formatted(kestrelMethod));
                    writer.popState();
                });
            writer.write("return builder.build();");

        }
    }

    private record ConvertFromGenerator(
        StructureGenerator generator, JavaWriter writer,
        Symbol smithySymbol, Model model
    ) implements Runnable {
        @Override
        public void run() {
            Stream.of(
                generator.getAllVarintMembers(),
                generator.getAllFourByteMembers(),
                generator.getAllEightByteMembers(),
                generator.getAllListMembers()
            )
                .flatMap(identity())
                .forEach(member -> {
                    Shape targetShape = model.expectShape(member.getTarget());
                    writer.pushState();
                    writer.putContext("optional", generator.isOptional(member));
                    String methodName = generator.methodNameForField(member);
                    writer.putContext("kestrelSetter", "set" + methodName);
                    writer.putContext(
                        "smithyGetter",
                        generator.getSymbolProvider().toSymbol(member).expectProperty("smithyMemberName", String.class)
                    );
                    writer.putContext("kestrelMemberSymbol", generator.getSymbolProvider().toSymbol(member));
                    String kestrelValue;
                    if (targetShape.isStructureShape() || targetShape.isUnionShape()) {
                        writer.putContext("transformation", true);
                        writer.putContext(
                            "convertedVariable",
                            "converted" + StringUtils.capitalize(member.getMemberName())
                        );
                        writer.putContext("transformer", new Runnable() {
                            @Override
                            public void run() {
                                writer.write("var ${convertedVariable:L} = new ${kestrelMemberSymbol:T}();");
                                writer.write("${convertedVariable:L}.convertFrom(o.${smithyGetter:L}());");
                            }
                        });
                        kestrelValue = "${convertedVariable:L}";
                    } else {
                        kestrelValue = "o.${smithyGetter:L}()";
                    }
                    writer.write("""
                        ${?optional}if (o.${smithyGetter:L}() != null) {
                            ${/optional}${?transformation}
                            ${transformer:C|}
                            ${/transformation}this.${kestrelSetter:L}(%s);${?optional}
                        }${/optional}
                        """.formatted(kestrelValue));
                    writer.popState();
                });
        }
    }
}
