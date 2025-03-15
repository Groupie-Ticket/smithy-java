/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.metrics.api;

import java.awt.Dimension;

/**
 * A Metric is used to measure something using a specific configuration.
 */
public sealed interface Metric {
    /**
     * Get the name of the metric.
     *
     * @return the name.
     */
    String name();

    /**
     * Get the unit of the metric.
     *
     * @return the metric unit.
     */
    MetricUnit unit();

    /**
     * Gets the list of dimensions of the metric.
     *
     * @return the dimensions of the metric.
     */
    Dimension[] dimensions();

    /**
     * Measures a count.
     *
     * @param name Name of the metric.
     * @param unit Metric unit.
     * @param dimensions Dimensions.
     */
    record Counter(String name, MetricUnit unit, Dimension... dimensions) implements Metric {}
}
