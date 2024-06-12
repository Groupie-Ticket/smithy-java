/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.http.binding;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;
import software.amazon.smithy.java.runtime.core.schema.Schema;
import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;
import software.amazon.smithy.java.runtime.core.serde.Codec;
import software.amazon.smithy.java.runtime.core.serde.EventEncoder;
import software.amazon.smithy.java.runtime.core.serde.SpecificShapeSerializer;

public final class AwsFlowShapeEncoder<T extends SerializableStruct> implements EventEncoder<T, AwsFlowFrame> {

    private final Schema eventSchema;
    private final Codec codec;

    public AwsFlowShapeEncoder(Schema eventSchema, Codec codec) {
        this.eventSchema = eventSchema;
        this.codec = codec;
    }

    @Override
    public AwsFlowFrame encode(T item) {
        var os = new ByteArrayOutputStream();
        var typeHolder = new AtomicReference<String>();
        try (var baseSerializer = codec.createSerializer(os)) {
            var possibleTypes = eventSchema.members().stream().map(Schema::memberName).collect(Collectors.toSet());

            item.serializeMembers(new SpecificShapeSerializer() {
                @Override
                public void writeStruct(Schema schema, SerializableStruct struct) {
                    if (possibleTypes.contains(schema.memberName())) {
                        typeHolder.compareAndSet(null, schema.memberName());
                    }
                    baseSerializer.writeStruct(schema, struct);
                }
            });
        }

        var headers = new HashMap<String, HeaderValue>();
        headers.put(":event-type", HeaderValue.fromString(typeHolder.get()));
        headers.put(":message-type", HeaderValue.fromString("event"));

        return new AwsFlowFrame(new Message(headers, os.toByteArray()));
    }
}
