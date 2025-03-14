/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.metrics.api.prototype;

public class ExampleTest {
    public void outer(Span span, Metrics metrics) {
        try (Span outer = span.newSpan("outer");
             Metrics childMetrics = metrics.newMetrics()) {
            span.addEvent("Starting outer");
            for (var i = 1; i <= 10; i++) {
                inner(outer, childMetrics);
            }
            span.addEvent("Ending outer");
        }
    }

    public void inner(Span span, Metrics metrics) {
        try (Span child = span.newSpan("recordCounter")) {
            var counter = metrics.newCounter("foo", MetricUnit.COUNT);
            for (var i = 1; i <= 10; i++) {
                counter.record(i, span);
            }
        }
    }
}
