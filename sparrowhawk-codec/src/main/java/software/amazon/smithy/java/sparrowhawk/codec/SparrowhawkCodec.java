/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.sparrowhawk.codec;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;
import software.amazon.smithy.java.runtime.core.schema.Schema;
import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;
import software.amazon.smithy.java.sparrowhawk.SparrowhawkDeserializer;
import software.amazon.smithy.java.sparrowhawk.SparrowhawkObject;
import software.amazon.smithy.java.sparrowhawk.SparrowhawkSerializer;

public abstract class SparrowhawkCodec {

    public final byte[] encodeException(Schema schema, Throwable exception) {
        var converted = convertException(exception);
        var sparrowhawkWrapper = new SparrowhawkExceptionWrapper();
        sparrowhawkWrapper.setSerializedError(ByteBuffer.wrap(serialize(converted)));
        sparrowhawkWrapper.setType(schema.id().toString());
        return serialize(sparrowhawkWrapper);
    }

    protected abstract SparrowhawkObject convertException(Throwable exception);

    public byte[] encode(SerializableStruct value) {
        throw new UnsupportedOperationException(this + " is an event streaming response");
    }

    public SerializableStruct decode(ByteBuffer buffer) {
        throw new UnsupportedOperationException(this + " is an event streaming request");
    }

    public SerializableStruct decodeInitialRequest(ByteBuffer buffer, Flow.Publisher<?> publisher) {
        throw new UnsupportedOperationException(this + " does not have an event streaming request");
    }

    protected final byte[] serialize(SparrowhawkObject object) {
        SparrowhawkSerializer s = new SparrowhawkSerializer(object.size());
        object.encodeTo(s);
        return s.payload();
    }

    protected final <T extends SerializableStruct> T deserialize(ByteBuffer buffer, SparrowhawkStructure<T> input) {
        SparrowhawkDeserializer d = new SparrowhawkDeserializer(buffer);
        input.decodeFrom(d);
        return input.convertTo();
    }

    protected final <T extends SerializableStruct> T deserializeStreaming(
        ByteBuffer buffer,
        SparrowhawkStructure<T> input,
        Flow.Publisher<?> publisher
    ) {
        SparrowhawkDeserializer d = new SparrowhawkDeserializer(buffer);
        input.decodeFrom(d);
        return input.convertTo(publisher);
    }

    public SerializableStruct deserializeEvent(ByteBuffer buffer) {
        throw new UnsupportedOperationException(this + " does not deserialize events");
    }

    public byte[] encodeEvent(SerializableStruct input) {
        throw new UnsupportedOperationException(this + " does not serialize events");
    }

    public byte[] encodeEventException(SerializableStruct input) {
        throw new UnsupportedOperationException(this + " does not serialize events");
    }

    public Flow.Publisher<? extends SerializableStruct> getStreamMember(SerializableStruct input) {
        throw new UnsupportedOperationException(this + " does not have an event streaming response");
    }

    protected final SparrowhawkObject createSynthetic(Throwable exception) {
        var sparrowhawk = new SparrowhawkFrameworkExceptionStructure();
        sparrowhawk.setMessage(exception.getMessage());
        return sparrowhawk;
    }
}
