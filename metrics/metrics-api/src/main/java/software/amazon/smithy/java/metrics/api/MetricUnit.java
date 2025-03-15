/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.metrics.api;

/**
 * The unit of measurement for a {@link Metric}.
 */
public enum MetricUnit {
    /**
     * Count of a single value.
     */
    COUNT,

    /**
     * A time measurement in milliseconds.
     */
    MILLISECONDS,

    /**
     * An unknown or custom metric unit.
     */
    UNKNOWN
}
