/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.kestrel;

import java.util.List;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.kestrel.codegen.*;
import software.amazon.smithy.kestrel.codegen.CodeSections.StartClassSection;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.CodeSection;

public class SmithyKestrelIntegration implements KestrelIntegration {


    @Override
    public SymbolProvider decorateSymbolProvider(Model model, KestrelSettings settings, SymbolProvider symbolProvider) {
        return new SmithySymbolVisitor(model, model.expectShape(settings.getService(), ServiceShape.class));
    }

    @Override
    public List<? extends CodeInterceptor<? extends CodeSection, JavaWriter>> interceptors(
        GenerationContext codegenContext
    ) {
        return List.of(new InterfaceGenerator(), new ConverterMethodsGenerator());
    }

    private static final class InterfaceGenerator implements CodeInterceptor<StartClassSection, JavaWriter> {

        @Override
        public Class<StartClassSection> sectionType() {
            return StartClassSection.class;
        }

        @Override
        public void write(JavaWriter writer, String s, StartClassSection section) {
            var generator = section.generator();
            var symbol = generator.getSymbol();
            var smithySymbol = symbol.expectProperty("smithySymbol", Symbol.class);
            if (generator.getShape().hasTrait(StreamingTrait.class)) {
                smithySymbol = CommonSymbols.Object.getSymbol();
            }
            writer.write(
                """
                    public final class $L implements $T<$T> {""",
                symbol.getName(),
                CommonSymbols.KestrelStructure,
                smithySymbol
            );
        }
    }
}
