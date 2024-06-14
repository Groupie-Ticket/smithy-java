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
    private static final String USE_INSTANT_FOR_TIMESTAMP = "useInstantForTimestamp";

    private final ShapeId service;
    private final boolean useInstant;

    private KestrelSettings(ShapeId service, boolean useInstant) {
        this.service = service;
        this.useInstant = useInstant;
    }

    public static KestrelSettings from(ObjectNode config) {
        config.warnIfAdditionalProperties(List.of(SERVICE));
        return new KestrelSettings(
            config.expectStringMember(SERVICE).expectShapeId(),
            config.expectBooleanMember(USE_INSTANT_FOR_TIMESTAMP).getValue()
        );
    }

    public ShapeId getService() {
        return Objects.requireNonNull(service, SERVICE + " not set");
    }

    public boolean useInstant() {
        return useInstant;
    }
}
