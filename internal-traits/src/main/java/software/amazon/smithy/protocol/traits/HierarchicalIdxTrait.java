/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.protocol.traits;

import java.util.List;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StringListTrait;

public final class HierarchicalIdxTrait extends StringListTrait {
    public static final ShapeId ID = ShapeId.from("smithy.protocols#hierarchicalIdx");

    public HierarchicalIdxTrait(List<String> value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
    }

    public HierarchicalIdxTrait(List<String> value) {
        this(value, SourceLocation.NONE);
    }

    public static final class Provider extends StringListTrait.Provider<HierarchicalIdxTrait> {
        public Provider() {
            super(ID, HierarchicalIdxTrait::new);
        }
    }
}
