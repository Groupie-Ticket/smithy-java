/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.sparrowhawk.codec;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import software.amazon.smithy.java.runtime.core.serde.InitialMessageDecoder;

public final class NonBufferingInitialEventDecoder extends InitialMessageDecoder<ByteBuffer> {
    public NonBufferingInitialEventDecoder(CompletableFuture<ByteBuffer> initialMessageCallback) {
        super(initialMessageCallback);
    }

    @Override
    protected void feed(ByteBuffer byteBuffer) throws Throwable {
        queue.add(byteBuffer);
    }
}
