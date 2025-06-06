/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.io.datastream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class WrappedDataStreamTest {
    @Test
    public void returnsByteBuffer() {
        var bytes = "foo".getBytes(StandardCharsets.UTF_8);
        var ds = DataStream.ofBytes(bytes);
        var wrapped = DataStream.ofPublisher(ds, "text/plain", 3);

        assertThat(wrapped.hasByteBuffer(), is(true));
        assertThat(wrapped.waitForByteBuffer(), equalTo(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8))));
    }
}
