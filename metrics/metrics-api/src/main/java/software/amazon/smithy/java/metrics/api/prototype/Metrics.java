/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.metrics.api.prototype;

import java.util.Map;
import software.amazon.smithy.java.metrics.api.Dimension;

public interface Metrics extends AutoCloseable {
    /**
     * Every metrics instance has a group.
     *
     * <p>Metrics created without a group default to the default group of "".
     */
    String DEFAULT_GROUP = "";

    @Override
    void close();

    default Metrics newMetrics() {
        return newMetrics(Map.of());
    }

    default Metrics newMetrics(Map<String, String> attributes) {
        return newMetrics(DEFAULT_GROUP);
    }

    default Metrics newMetrics(String group) {
        return newMetrics(group, Map.of());
    }

    Metrics newMetrics(String group, Map<String, String> attributes);

    default DoubleInstrument newCounter(String name, Dimension... dimensions) {
        return newCounter(name, MetricUnit.COUNT, dimensions);
    }

    DoubleInstrument newCounter(String name, MetricUnit unit, Dimension... dimensions);

    DoubleInstrument newHistogram(String name, MetricUnit unit, Dimension... dimensions);

    DoubleInstrument newLevel(String name, MetricUnit unit, Dimension... dimensions);

    DoubleInstrument newRatio(String name, MetricUnit unit, Dimension... dimensions);
}
