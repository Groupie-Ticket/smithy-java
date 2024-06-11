/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.http.binding;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import software.amazon.eventstream.Message;
import software.amazon.smithy.java.runtime.core.serde.FrameDecoder;

public class AwsFlowFrameDecoder implements FrameDecoder<AwsFlowFrame> {
    @Override
    public List<AwsFlowFrame> decode(ByteBuffer buffer) {
        List<AwsFlowFrame> frames = new ArrayList<>();
        while (buffer.remaining() > AwsFlowFramePrelude.LENGTH_WITH_CRC) {
            var prelude = AwsFlowFramePrelude.decode(buffer);
            if (AwsFlowFramePrelude.LENGTH_WITH_CRC + prelude.getTotalLength() >= buffer.remaining()) {
                Message decoded = Message.decode(buffer);
                // TODO: hack: sigv4 encoded messages are double-wrapped, this just unwraps them blindly for now
                if (decoded.getHeaders().containsKey(":chunk-signature")) {
                    if (decoded.getPayload().length == 0) {
                        // TODO end of sigv4 stream is an empty chunk?
                        break;
                    }
                    decoded = Message.decode(ByteBuffer.wrap(decoded.getPayload()));
                }
                frames.add(new AwsFlowFrame(decoded));
            }
        }
        return frames;
    }
}
