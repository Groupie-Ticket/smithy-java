/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.metrics.api;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

final class MetricsNode implements Metrics {

    private final Consumer<Measurements> reporter;
    private final MetricsNode parent;
    private final String spanId;
    private int metricsState = 0;
    private Map<String, String> attributes;

    private MetricsNode(Consumer<Measurements> reporter, MetricsNode parent, String spanId) {
        this.reporter = reporter;
        this.parent = parent;
        this.spanId = spanId;
    }

    static MetricsNode root(Consumer<Measurements> reporter) {
        return root(reporter, null);
    }

    static MetricsNode root(Consumer<Measurements> reporter, String span) {
        return new MetricsNode(reporter, null, span);
    }

    static MetricsNode child(MetricsNode parent) {
        return child(parent, null);
    }

    static MetricsNode child(MetricsNode parent, String span) {
        return new MetricsNode(null, parent, span);
    }

    @Override
    public Metrics newMetrics() {
        return newSpan(null);
    }

    @Override
    public Metrics newSpan(String span) {
        assertNotClosed("Attempted to create a new Metrics instance from a closed MetricsNode");
        metricsState++;
        return child(this, span);
    }

    @Override
    public void close() {
        assertNotClosed("Attempted to close a MetricsNode more than once");
        if (metricsState > 0) {
            // warn about leaked metrics due to unclosed children.
        }

        metricsState = -1;
        if (parent != null) {
            parent.closeChild(this);
        }
    }

    void closeChild(MetricsNode child) {
        if (metricsState <= 0) {
            throw new IllegalStateException("Attempted to close a non-existent child of a MetricsNode");
        }
        metricsState--;
        // TODO: merge the measurements of the child into the parent.
    }

    void assertNotClosed(String errorMessage) {
        if (metricsState == -1) {
            throw new IllegalStateException("Attempted to update a closed Metrics instance: " + errorMessage);
        }
    }

    @Override
    public Instrument newCounter(Metric.Counter metric) {
        throw new UnsupportedOperationException("TODO");
    }

    String spanId() {
        return spanId;
    }

    @Override
    public String traceId() {
        return parent != null ? parent.traceId() : null;
    }

    @Override
    public void setAttribute(String key, String value) {
        assertNotClosed("Attempted to set an attribute on a closed MetricsNode");
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(key, value);
    }
}
