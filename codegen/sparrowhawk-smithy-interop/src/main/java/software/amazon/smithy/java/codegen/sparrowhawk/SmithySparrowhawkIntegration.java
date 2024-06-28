/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.sparrowhawk;

import static software.amazon.smithy.java.codegen.sparrowhawk.InteropSymbolProperties.SMITHY_SYMBOL;
import static software.amazon.smithy.sparrowhawk.codegen.CommonSymbols.imp;

import java.io.File;
import java.util.List;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.SymbolReference;
import software.amazon.smithy.java.codegen.sparrowhawk.generators.ConverterMethodsGenerator;
import software.amazon.smithy.java.codegen.sparrowhawk.generators.SparrowhawkCodecFactoryGenerator;
import software.amazon.smithy.java.sparrowhawk.codec.SparrowhawkCodecFactory;
import software.amazon.smithy.java.sparrowhawk.codec.SparrowhawkStructure;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.sparrowhawk.codegen.CodeSections.StartClassSection;
import software.amazon.smithy.sparrowhawk.codegen.GenerationContext;
import software.amazon.smithy.sparrowhawk.codegen.JavaWriter;
import software.amazon.smithy.sparrowhawk.codegen.SparrowhawkIntegration;
import software.amazon.smithy.sparrowhawk.codegen.SparrowhawkSettings;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.CodeSection;

public class SmithySparrowhawkIntegration implements SparrowhawkIntegration {

    private static final SymbolReference SPARROWHAWK_STRUCTURE = imp(SparrowhawkStructure.class);


    @Override
    public SymbolProvider decorateSymbolProvider(
        Model model,
        SparrowhawkSettings settings,
        SymbolProvider symbolProvider
    ) {
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
        var codecGenerator = new SparrowhawkCodecFactoryGenerator(service, model, symbolProvider);
        var delegator = codegenContext.writerDelegator();
        var fileName = service.getId().getNamespace().replaceAll("\\.", File.separator)
            + File.separator + "sparrowhawk" + File.separator + codecGenerator.className() + ".java";
        delegator.useFileWriter(fileName, service.getId().getNamespace() + ".sparrowhawk", codecGenerator);
        delegator.useFileWriter("META-INF/services/" + SparrowhawkCodecFactory.class.getName(), w -> {
            w.setPlain(true);
            w.write(service.getId().getNamespace() + ".sparrowhawk." + codecGenerator.className());
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
            writer.write(
                """
                    public final class $L implements $T<$T> {""",
                symbol.getName(),
                SPARROWHAWK_STRUCTURE,
                smithySymbol
            );
        }
    }
}
