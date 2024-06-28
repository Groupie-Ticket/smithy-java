/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.sparrowhawk.codec;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import software.amazon.smithy.java.runtime.core.serde.InitialMessageDecoder;
import software.amazon.smithy.java.sparrowhawk.Bufferer;

public final class BufferingSparrowhawkInitialEventDecoder extends InitialMessageDecoder<ByteBuffer> {
    private final Bufferer bufferer;

    public BufferingSparrowhawkInitialEventDecoder(CompletableFuture<ByteBuffer> callback) {
        super(callback);
        this.bufferer = new Bufferer(b -> queue.add(ByteBuffer.wrap(b)));
    }

    @Override
    protected void feed(ByteBuffer byteBuffer) throws Throwable {
        bufferer.feed(byteBuffer);
    }
}
