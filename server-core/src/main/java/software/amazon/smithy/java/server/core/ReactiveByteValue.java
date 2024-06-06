/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;


import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

public final class ReactiveByteValue implements Value<Flow.Publisher<ByteBuffer>> {
    private final Flow.Publisher<ByteBuffer> value;

    public ReactiveByteValue(Flow.Publisher<ByteBuffer> value) {
        this.value = value;
    }

    @Override
    public Flow.Publisher<ByteBuffer> get() {
        return value;
    }

}
