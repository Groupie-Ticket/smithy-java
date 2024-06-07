/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.kestrel.codegen;

import java.util.List;
import java.util.Objects;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

public final class KestrelSettings {
    private static final String SERVICE = "service";

    private ShapeId service;

    public static KestrelSettings from(ObjectNode config) {
        KestrelSettings settings = new KestrelSettings();
        config.warnIfAdditionalProperties(List.of(SERVICE));

        settings.setService(config.expectStringMember(SERVICE).expectShapeId());

        return settings;
    }

    public ShapeId getService() {
        return Objects.requireNonNull(service, SERVICE + " not set");
    }

    public void setService(ShapeId service) {
        this.service = Objects.requireNonNull(service);
    }
}
