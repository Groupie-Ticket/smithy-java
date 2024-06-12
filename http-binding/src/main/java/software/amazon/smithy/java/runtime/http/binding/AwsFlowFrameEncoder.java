/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.http.binding;

import java.nio.ByteBuffer;
import software.amazon.smithy.java.runtime.core.serde.FrameEncoder;

public class AwsFlowFrameEncoder implements FrameEncoder<AwsFlowFrame> {

    @Override
    public ByteBuffer encode(AwsFlowFrame frame) {
        return frame.unwrap().toByteBuffer();
    }
}
