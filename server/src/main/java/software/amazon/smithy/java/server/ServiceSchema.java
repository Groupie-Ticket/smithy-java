/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server;

import java.util.Collections;
import java.util.List;
import software.amazon.smithy.model.shapes.ShapeId;

public final class ServiceSchema {

    private final ShapeId id;
    private final List<ShapeId> supportedProtocols;
    private final List<ShapeId> awsAuthModes;

    public ShapeId getId() {
        return id;
    }

    public List<ShapeId> getSupportedProtocols() {
        return supportedProtocols;
    }

    public List<ShapeId> getAwsAuthModes() {
        return awsAuthModes;
    }

    private ServiceSchema(Builder builder) {
        this.id = builder.id;
        this.supportedProtocols = Collections.unmodifiableList(builder.supportedProtocols);
        this.awsAuthModes = Collections.unmodifiableList(builder.awsAuthModes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private ShapeId id;
        private List<ShapeId> supportedProtocols;
        private List<ShapeId> awsAuthModes = Collections.emptyList();

        public Builder id(ShapeId shapeId) {
            this.id = shapeId;
            return this;
        }

        public Builder supportedProtocols(List<ShapeId> supportedProtocols) {
            this.supportedProtocols = supportedProtocols;
            return this;
        }

        public Builder awsAuthModes(List<ShapeId> awsAuthModes) {
            this.awsAuthModes = awsAuthModes;
            return this;
        }

        public ServiceSchema build() {
            return new ServiceSchema(this);
        }
    }
}
