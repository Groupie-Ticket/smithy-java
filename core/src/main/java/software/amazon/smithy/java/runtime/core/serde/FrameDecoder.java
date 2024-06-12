/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core.serde;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Decodes frames from bytes.
 */
public interface FrameDecoder<F extends Frame<?>> {
    /**
     * Decode 0 or more frames from a buffer, leaving extra data at the end of the buffer for subsequent consumption.
     * @param buffer the buffer to attempt to read frames from
     * @return all the frames readable from the buffer
     */
    List<F> decode(ByteBuffer buffer);
}
