/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server;

import java.util.List;
import software.amazon.smithy.model.shapes.ShapeId;

public final class ServiceSchema {

    private final ShapeId id;
    private final List<ShapeId> supportedProtocols;


    private ServiceSchema(Builder builder) {
        this.id = builder.id;
        this.supportedProtocols = builder.supportedProtocols;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private ShapeId id;
        private List<ShapeId> supportedProtocols;

        public Builder id(ShapeId shapeId) {
            this.id = shapeId;
            return this;
        }

        public Builder supportedProtocols(List<ShapeId> supportedProtocols) {
            this.supportedProtocols = supportedProtocols;
            return this;
        }

        public ServiceSchema build() {
            return new ServiceSchema(this);
        }
    }
}
