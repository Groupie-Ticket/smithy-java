/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.kestrel;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;

public final class KestrelFieldTrait extends AbstractTrait {
    static final ShapeId ID = ShapeId.fromParts("kestrel", "field");

    private final FieldType type;
    private final int fieldSetIdx;
    private final int typeIdx;
    private final boolean required;

    public KestrelFieldTrait(FieldType type, int fieldSetIdx, int typeIdx, boolean required) {
        super(ID, Node.objectNode());
        if (fieldSetIdx < 0) { throw new IllegalArgumentException("illegal fieldSetIdx " + fieldSetIdx); }
        if (typeIdx < 1 || typeIdx > 61) { throw new IllegalArgumentException("Illegal typeIdx" + typeIdx); }
        this.type = type;
        this.fieldSetIdx = fieldSetIdx;
        this.typeIdx = typeIdx;
        this.required = required;
    }

    public FieldType getType() {
        return type;
    }

    public int getFieldSetIdx() {
        return fieldSetIdx;
    }

    public int getTypeIdx() {
        return typeIdx;
    }

    public boolean isRequired() {
        return required;
    }

    @Override
    protected Node createNode() {
        return Node.objectNode();
    }

    @Override
    public boolean isSynthetic() {
        return true;
    }
}
