/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.metrics.api;

/**
 * An Instrument is used to record values of a {@link Metric} to a {@link Metrics}.
 */
public interface Instrument {
    /**
     * Records a value.
     *
     * @param value the value to record.
     */
    void record(double value);
}
