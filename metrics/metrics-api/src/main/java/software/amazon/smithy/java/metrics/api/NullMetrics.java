/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.metrics.api;

final class NullMetrics implements Metrics {
    @Override
    public String traceId() {
        return null;
    }

    @Override
    public void close() {}

    @Override
    public void setAttribute(String key, String value) {}

    @Override
    public Metrics newMetrics() {
        return this;
    }

    @Override
    public Metrics newSpan(String span) {
        return this;
    }

    @Override
    public Instrument newCounter(Metric.Counter metric) {
        return value -> {};
    }
}
