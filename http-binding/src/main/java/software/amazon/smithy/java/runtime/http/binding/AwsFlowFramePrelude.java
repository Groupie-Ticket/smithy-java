/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.http.binding;

import static java.lang.String.format;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

// Duplicated from the flow package, for visibility issues
final class AwsFlowFramePrelude {
    static final int LENGTH = 8;
    static final int LENGTH_WITH_CRC = LENGTH + 4;

    private final int totalLength;
    private final long headersLength;

    private AwsFlowFramePrelude(int totalLength, long headersLength) {
        this.totalLength = totalLength;
        this.headersLength = headersLength;
    }

    static AwsFlowFramePrelude decode(ByteBuffer buf) {
        buf = buf.duplicate();

        long computedPreludeCrc = computePreludeCrc(buf);

        long totalLength = Integer.toUnsignedLong(buf.getInt());
        long headersLength = Integer.toUnsignedLong(buf.getInt());
        long wirePreludeCrc = Integer.toUnsignedLong(buf.getInt());
        if (computedPreludeCrc != wirePreludeCrc) {
            throw new IllegalArgumentException(
                format(
                    "Prelude checksum failure: expected 0x%x, computed 0x%x",
                    wirePreludeCrc,
                    computedPreludeCrc
                )
            );
        }

        if (headersLength < 0 || headersLength > 131_072) {
            throw new IllegalArgumentException("Illegal headers_length value: " + headersLength);
        }

        long payloadLength = (totalLength - headersLength) - 16;
        // This implementation temporarily accepts larger payloads than the spec permits.
        if (payloadLength < 0 || payloadLength > 25_165_824) {
            throw new IllegalArgumentException("Illegal payload size: " + payloadLength);
        }

        return new AwsFlowFramePrelude(Math.toIntExact(totalLength), headersLength);
    }

    private static long computePreludeCrc(ByteBuffer buf) {
        byte[] prelude = new byte[AwsFlowFramePrelude.LENGTH];
        buf.duplicate().get(prelude);

        Checksum crc = new CRC32();
        crc.update(prelude, 0, prelude.length);
        return crc.getValue();
    }

    int getTotalLength() {
        return totalLength;
    }

    long getHeadersLength() {
        return headersLength;
    }
}
