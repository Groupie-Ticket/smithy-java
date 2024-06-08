/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.kestrel;

import static java.util.function.Function.identity;

import java.util.stream.Stream;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.kestrel.codegen.CodeSections.EndClassSection;
import software.amazon.smithy.kestrel.codegen.JavaWriter;
import software.amazon.smithy.kestrel.codegen.StructureGenerator;
import software.amazon.smithy.utils.CodeInterceptor;

public class ConverterMethodsGenerator implements CodeInterceptor<EndClassSection, JavaWriter> {

    @Override
    public Class<EndClassSection> sectionType() {
        return EndClassSection.class;
    }

    @Override
    public void write(JavaWriter writer, String s, EndClassSection section) {
        var generator = section.generator();
        var shape = generator.getShape();
        var model = generator.getModel();
        var symbolProvider = generator.getSymbolProvider();
        var smithySymbol = generator.getSymbol().expectProperty("smithySymbol", Symbol.class);

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
            public void convertFrom(${smithySymbol:T} object) {
                ${convertFrom:C|}
            }
            """;

        writer.pushState();
        writer.putContext("smithySymbol", smithySymbol);
        writer.putContext("convertTo", new ConvertToGenerator(generator, writer, smithySymbol));
        writer.putContext("convertFrom", new ConvertFromGenerator());
        writer.write(template);
        writer.popState();

    }

    private record ConvertToGenerator(
        StructureGenerator generator, JavaWriter writer,
        Symbol smithySymbol
    ) implements Runnable {

        @Override
        public void run() {
            writer.write("var builder = ${smithySymbol:T}.builder();");
            Stream.of(
                generator.getAllVarintMembers(),
                generator.getAllFourByteMembers(),
                generator.getAllEightByteMembers()
            )
                .flatMap(identity())
                .forEach(member -> {
                    writer.pushState();
                    writer.putContext("optional", generator.isOptional(member));
                    writer.putContext("kestrelHas", "has" + generator.methodNameForField(member));
                    writer.putContext("kestrelFieldName", generator.fieldName(member));
                    writer.putContext(
                        "builderMethod",
                        generator.getSymbolProvider().toSymbol(member).expectProperty("smithyMemberName", String.class)
                    );
                    writer.write("""
                        ${?optional}if (${kestrelHas:L}()) {
                            ${/optional}builder.${builderMethod:L}(${kestrelFieldName:L});${?optional}
                        }${/optional}
                        """);
                    writer.popState();
                });
            writer.write("return builder.build();");
        }
    }

    private record ConvertFromGenerator() implements Runnable {
        @Override
        public void run() {

        }
    }
}
