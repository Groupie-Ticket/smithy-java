/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.metrics.api.prototype;

import java.util.Map;

public interface DoubleInstrument {
    default void record(double value) {
        record(value, Map.of());
    }

    default void record(double value, Span span) {
        record(value, Map.of(), span);
    }

    default void record(double value, Map<String, String> attributes) {
        record(value, attributes, null);
    }

    void record(double value, Map<String, String> attributes, Span span);
}
