/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.kestrel;

import static software.amazon.smithy.java.codegen.kestrel.InteropSymbolProperties.SMITHY_SYMBOL;
import static software.amazon.smithy.kestrel.codegen.CommonSymbols.imp;

import java.io.File;
import java.util.List;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.SymbolReference;
import software.amazon.smithy.java.codegen.kestrel.generators.ConverterMethodsGenerator;
import software.amazon.smithy.java.codegen.kestrel.generators.KestrelCodecFactoryGenerator;
import software.amazon.smithy.java.kestrel.codec.KestrelCodecFactory;
import software.amazon.smithy.java.kestrel.codec.KestrelStructure;
import software.amazon.smithy.kestrel.codegen.*;
import software.amazon.smithy.kestrel.codegen.CodeSections.StartClassSection;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.CodeSection;

public class SmithyKestrelIntegration implements KestrelIntegration {

    private static SymbolReference KESTREL_STRUCTURE = imp(KestrelStructure.class);


    @Override
    public SymbolProvider decorateSymbolProvider(Model model, KestrelSettings settings, SymbolProvider symbolProvider) {
        return new SmithySymbolVisitor(model, model.expectShape(settings.getService(), ServiceShape.class), settings);
    }

    @Override
    public List<? extends CodeInterceptor<? extends CodeSection, JavaWriter>> interceptors(
        GenerationContext codegenContext
    ) {
        return List.of(new InterfaceGenerator(), new ConverterMethodsGenerator());
    }

    @Override
    public void customize(GenerationContext codegenContext) {
        var model = codegenContext.model();
        var service = (ServiceShape) model.expectShape(codegenContext.settings().getService());
        var symbolProvider = codegenContext.symbolProvider();
        var codecGenerator = new KestrelCodecFactoryGenerator(service, model, symbolProvider);
        var delegator = codegenContext.writerDelegator();
        var fileName = service.getId().getNamespace().replaceAll("\\.", File.separator)
            + File.separator + "kestrel" + File.separator + codecGenerator.className() + ".java";
        delegator.useFileWriter(fileName, service.getId().getNamespace() + ".kestrel", codecGenerator);
        delegator.useFileWriter("META-INF/services/" + KestrelCodecFactory.class.getName(), w -> {
            w.setPlain(true);
            w.write(service.getId().getNamespace() + ".kestrel." + codecGenerator.className());
        });
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
            var smithySymbol = symbol.expectProperty(SMITHY_SYMBOL);
            if (generator.getShape().hasTrait(StreamingTrait.class)) {
                writer.write("public final class $L implements $T {", symbol.getName(), CommonSymbols.KestrelObject);
                return;
            }
            writer.write(
                """
                    public final class $L implements $T<$T> {""",
                symbol.getName(),
                KESTREL_STRUCTURE,
                smithySymbol
            );
        }
    }
}
