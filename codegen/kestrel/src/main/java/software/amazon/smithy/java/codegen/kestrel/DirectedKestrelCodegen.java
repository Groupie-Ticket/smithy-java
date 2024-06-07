/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.kestrel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.directed.*;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NullableIndex;
import software.amazon.smithy.model.shapes.*;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.protocol.traits.IdxTrait;

public final class DirectedKestrelCodegen implements
    DirectedCodegen<GenerationContext, KestrelSettings, KestrelIntegration> {
    @Override
    public SymbolProvider createSymbolProvider(CreateSymbolProviderDirective<KestrelSettings> directive) {
        var model = directive.model();
        var service = model.expectShape(directive.settings().getService(), ServiceShape.class);
        return new KestrelSymbolVisitor(model, service);
    }

    @Override
    public GenerationContext createContext(CreateContextDirective<KestrelSettings, KestrelIntegration> directive) {
        return GenerationContext.builder()
            .model(preprocess(directive.model()))
            .fileManifest(directive.fileManifest())
            .integrations(directive.integrations())
            .symbolProvider(directive.symbolProvider())
            .settings(directive.settings())
            .writerDelegator(
                new JavaDelegator(directive.fileManifest(), directive.symbolProvider(), directive.settings())
            )
            .build();
    }

    @Override
    public void generateService(GenerateServiceDirective<GenerationContext, KestrelSettings> directive) {

    }

    @Override
    public void generateOperation(GenerateOperationDirective<GenerationContext, KestrelSettings> directive) {

    }

    @Override
    public void generateStructure(GenerateStructureDirective<GenerationContext, KestrelSettings> directive) {
        generate(directive);
    }

    private static void generate(ShapeDirective<? extends Shape, GenerationContext, KestrelSettings> directive) {

        directive.context().writerDelegator().useShapeWriter(directive.shape(), writer -> {
            new StructureGenerator(
                directive.shape(),
                directive.model(),
                directive.context().symbolProvider(),
                writer
            ).run();
        });
    }

    @Override
    public void generateError(GenerateErrorDirective<GenerationContext, KestrelSettings> directive) {
        generate(directive);
    }

    @Override
    public void generateUnion(GenerateUnionDirective<GenerationContext, KestrelSettings> directive) {
        generate(directive);
    }

    @Override
    public void generateEnumShape(GenerateEnumDirective<GenerationContext, KestrelSettings> directive) {

    }

    @Override
    public void generateIntEnumShape(GenerateIntEnumDirective<GenerationContext, KestrelSettings> directive) {

    }

    private static Model preprocess(Model model) {
        var newShapes = new ArrayList<MemberShape>();
        for (StructureShape shape : model.getStructureShapes()) {
            transformMembers(model, shape, newShapes);
        }
        for (UnionShape shape : model.getUnionShapes()) {
            transformMembers(model, shape, newShapes);
        }
        var transform = ModelTransformer.create();
        return transform.replaceShapes(model, newShapes);
    }

    private static void transformMembers(Model model, Shape shape, List<MemberShape> newShapes) {
        NullableIndex idx = NullableIndex.of(model);
        List<MemberShape> members = new ArrayList<>(shape.members());
        if (members.isEmpty() || members.stream().noneMatch(m -> m.hasTrait(IdxTrait.class))) {
            return;
        }
        members.sort(
            Comparator.comparingInt(
                m -> m.getTrait(IdxTrait.class)
                    .map(IdxTrait::getValue)
                    .orElse(0)
            )
        );

        var membersByType = new HashMap<FieldType, List<MemberShape>>();

        for (MemberShape ms : members) {
            FieldType type;
            switch (model.expectShape((ms.getTarget())).getType()) {
                case BLOB, STRING, DOCUMENT, BIG_DECIMAL, BIG_INTEGER, ENUM, LIST, SET, MAP, STRUCTURE, UNION ->
                    type = FieldType.LIST;
                case BOOLEAN, BYTE, SHORT, INTEGER, LONG, INT_ENUM -> type = FieldType.VARINT;
                case TIMESTAMP, DOUBLE -> type = FieldType.EIGHT_BYTE;
                case FLOAT -> type = FieldType.FOUR_BYTE;
                default -> throw new IllegalArgumentException();
            }
            membersByType.computeIfAbsent(type, $ -> new ArrayList<>()).add(ms);
        }
        for (var e : membersByType.entrySet()) {
            List<MemberShape> value = e.getValue();
            for (int i = 0; i < value.size(); i++) {
                var ms = value.get(i);
                newShapes.add(
                    ms.toBuilder()
                        .addTrait(new KestrelFieldTrait(e.getKey(), i / 61, i % 61 + 1, !idx.isMemberNullable(ms)))
                        .build()
                );
            }
        }
    }
}
