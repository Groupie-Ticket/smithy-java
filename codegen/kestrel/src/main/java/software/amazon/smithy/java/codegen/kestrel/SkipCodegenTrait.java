/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.kestrel;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;

public final class SkipCodegenTrait extends AbstractTrait {
    public static final ShapeId TRAIT_ID = ShapeId.fromParts("kestrel", "skipCodegen");

    public SkipCodegenTrait() {
        super(TRAIT_ID, Node.nullNode());
    }

    @Override
    protected Node createNode() {
        return null;
    }

    @Override
    public boolean isSynthetic() {
        return true;
    }
}
