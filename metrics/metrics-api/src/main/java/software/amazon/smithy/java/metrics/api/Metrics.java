/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.metrics.api;

/**
 * Aggregates instruments and measurements.
 */
public interface Metrics extends AutoCloseable {
    /**
     * Get the trace ID of the metrics instance, or null if not set.
     *
     * @return the trace ID or null.
     */
    String traceId();

    /**
     * Set an attribute on this Metrics instance that is applied to every instrument measurement.
     *
     * @param key Key to set.
     * @param value Value to set.
     */
    void setAttribute(String key, String value);

    /**
     * Closes the metrics instances and sends the collected metrics to the parent, or to a reporter.
     */
    @Override
    void close();

    /**
     * Create a metrics instance.
     *
     * @return the created child metrics instance.
     */
    Metrics newMetrics();

    /**
     * Create a child metrics instance for a span.
     *
     * <p>When the child instance is closed, it sends its aggregated metrics to the parent.
     *
     * @return the created child metrics instance.
     */
    Metrics newSpan(String span);

    /**
     * Get or create an {@link Instrument} used to record measurements for the given {@link Metric}.
     *
     * @param metric The metric to instrument.
     * @return The instrument.
     */
    Instrument newCounter(Metric.Counter metric);
}
