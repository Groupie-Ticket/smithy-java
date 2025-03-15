/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.metrics.api;

public class ExampleTest {

    private static final Metric.Counter REQUEST_COUNT = new Metric.Counter("requestCount", MetricUnit.COUNT);

    public void example(MetricsFactory factory) {
        try (Metrics outer = factory.newSpan("outer")) {
            var instrument = outer.newCounter(REQUEST_COUNT);
            for (var i = 1; i <= 10; i++) {
                instrument.record(1);
            }
        }
    }
}
