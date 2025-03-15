/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.metrics.api;

/**
 * Creates new {@link Metrics} instances.
 */
public interface MetricsFactory {
    /**
     * Create a metrics instance.
     *
     * @return the created instance.
     */
    Metrics newMetrics();

    /**
     * Create a metrics instance with a span.
     *
     * @param span Span to create.
     * @return the created Metrics instance.
     */
    Metrics newSpan(String span);
}
