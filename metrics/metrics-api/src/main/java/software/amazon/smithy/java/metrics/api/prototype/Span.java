/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.metrics.api.prototype;

public interface Span extends AutoCloseable {
    String spanId();
    String traceId();
    Span setAttribute(String key, String value);
    Span newSpan(String name);
    Span addEvent(String event);

    @Override
    void close();
}
