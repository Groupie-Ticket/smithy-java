/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.http.binding;

import software.amazon.eventstream.Message;
import software.amazon.smithy.java.runtime.core.serde.Frame;

public final class AwsFlowFrame implements Frame<Message> {

    private final Message message;

    AwsFlowFrame(Message message) {
        this.message = message;
    }

    @Override
    public Message unwrap() {
        return message;
    }
}
