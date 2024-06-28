/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.sparrowhawk;

import static software.amazon.smithy.java.codegen.sparrowhawk.InteropSymbolProperties.SMITHY_MEMBER;
import static software.amazon.smithy.java.codegen.sparrowhawk.InteropSymbolProperties.SMITHY_SYMBOL;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.java.codegen.JavaSymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.*;
import software.amazon.smithy.sparrowhawk.codegen.SparrowhawkSettings;
import software.amazon.smithy.sparrowhawk.codegen.SparrowhawkSymbolVisitor;


public class SmithySymbolVisitor extends SparrowhawkSymbolVisitor {

    private final JavaSymbolProvider smithySymbolProvider;

    public SmithySymbolVisitor(Model model, ServiceShape service, SparrowhawkSettings settings) {
        super(model, service, settings);
        this.smithySymbolProvider = new JavaSymbolProvider(model, service, service.getId().getNamespace());
    }

    @Override
    public Symbol memberShape(MemberShape shape) {
        return super.memberShape(shape).toBuilder()
            .putProperty(SMITHY_SYMBOL, smithySymbolProvider.memberShape(shape))
            .putProperty(SMITHY_MEMBER, smithySymbolProvider.toMemberName(shape))
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
