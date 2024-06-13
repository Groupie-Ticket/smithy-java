/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.kestrel;

import static software.amazon.smithy.java.codegen.kestrel.InteropSymbolProperties.SMITHY_SYMBOL;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.java.codegen.JavaSymbolProvider;
import software.amazon.smithy.kestrel.codegen.KestrelSymbolVisitor;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.*;


public class SmithySymbolVisitor extends KestrelSymbolVisitor {

    private final JavaSymbolProvider smithySymbolProvider;

    public SmithySymbolVisitor(Model model, ServiceShape service) {
        super(model, service);
        this.smithySymbolProvider = new JavaSymbolProvider(model, service, service.getId().getNamespace());
    }

    @Override
    public Symbol memberShape(MemberShape shape) {
        return super.memberShape(shape).toBuilder()
            .putProperty(SMITHY_SYMBOL, smithySymbolProvider.memberShape(shape))
            .putProperty("smithyMemberName", smithySymbolProvider.toMemberName(shape))
            .build();
    }

    @Override
    protected Symbol.Builder structureSymbolBuilder(StructureShape shape) {
        return super.structureSymbolBuilder(shape).putProperty(
            SMITHY_SYMBOL,
            smithySymbolProvider.structureShape(shape)
        );
    }

    @Override
    protected Symbol.Builder unionSymbolBuilder(UnionShape shape) {
        return super.unionSymbolBuilder(shape).putProperty(SMITHY_SYMBOL, smithySymbolProvider.unionShape(shape));
    }

}
