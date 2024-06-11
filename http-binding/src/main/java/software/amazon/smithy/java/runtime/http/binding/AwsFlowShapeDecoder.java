/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.http.binding;

import java.util.function.Supplier;
import software.amazon.eventstream.Message;
import software.amazon.smithy.java.runtime.core.schema.Schema;
import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;
import software.amazon.smithy.java.runtime.core.schema.ShapeBuilder;
import software.amazon.smithy.java.runtime.core.serde.Codec;
import software.amazon.smithy.java.runtime.core.serde.EventDecoder;

public final class AwsFlowShapeDecoder<T extends SerializableStruct> implements EventDecoder<AwsFlowFrame, T> {

    private final Supplier<ShapeBuilder<T>> eventBuilder;
    private final Schema eventSchema;
    private final Codec codec;

    public AwsFlowShapeDecoder(Supplier<ShapeBuilder<T>> eventBuilder, Schema eventSchema, Codec codec) {
        this.eventBuilder = eventBuilder;
        this.eventSchema = eventSchema;
        this.codec = codec;
    }

    @Override
    public T decode(AwsFlowFrame frame) {
        Message message = frame.unwrap();
        String messageType = getMessageType(message);
        if (!messageType.equals("event")) {
            throw new UnsupportedOperationException("Unsupported frame type: " + messageType);
        }
        String eventType = getEventType(message);
        Schema memberSchema = eventSchema.member(eventType);
        if (memberSchema == null) {
            throw new IllegalArgumentException("Unsupported event type: " + eventType);
        }

        return eventBuilder.get()
            .deserialize(
                new AwsFlowEventDeserializer(
                    memberSchema,
                    codec.createDeserializer(message.getPayload())
                )
            )
            .build();
    }

    private String getEventType(Message message) {
        return message.getHeaders().get(":event-type").getString();
    }

    private String getMessageType(Message message) {
        return message.getHeaders().get(":message-type").getString();
    }
}
