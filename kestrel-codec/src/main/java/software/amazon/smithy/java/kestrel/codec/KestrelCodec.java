/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.kestrel.codec;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;
import software.amazon.smithy.java.kestrel.KestrelDeserializer;
import software.amazon.smithy.java.kestrel.KestrelObject;
import software.amazon.smithy.java.kestrel.KestrelSerializer;
import software.amazon.smithy.java.runtime.core.schema.Schema;
import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;

public abstract class KestrelCodec {

    public final byte[] encodeException(Schema schema, Throwable exception) {
        var converted = convertException(exception);
        var kestrelWrapper = new KestrelExceptionWrapper();
        kestrelWrapper.setSerializedError(ByteBuffer.wrap(serialize(converted)));
        kestrelWrapper.setType(schema.id().toString());
        return serialize(kestrelWrapper);
    }

    protected abstract KestrelObject convertException(Throwable exception);

    public byte[] encode(SerializableStruct value) {
        throw new UnsupportedOperationException(this + " is an event streaming response");
    }

    public SerializableStruct decode(ByteBuffer buffer) {
        throw new UnsupportedOperationException(this + " is an event streaming request");
    }

    public SerializableStruct decodeInitialRequest(ByteBuffer buffer, Flow.Publisher<?> publisher) {
        throw new UnsupportedOperationException(this + " does not have an event streaming request");
    }

    protected final byte[] serialize(KestrelObject object) {
        KestrelSerializer s = new KestrelSerializer(object.size());
        object.encodeTo(s);
        return s.payload();
    }

    protected final <T extends SerializableStruct> T deserialize(ByteBuffer buffer, KestrelStructure<T> input) {
        KestrelDeserializer d = new KestrelDeserializer(buffer);
        input.decodeFrom(d);
        return input.convertTo();
    }

    protected final <T extends SerializableStruct> T deserializeStreaming(
        ByteBuffer buffer,
        KestrelStructure<T> input,
        Flow.Publisher<?> publisher
    ) {
        KestrelDeserializer d = new KestrelDeserializer(buffer);
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

    protected final KestrelObject createSynthetic(Throwable exception) {
        var kestrel = new KestrelFrameworkExceptionStructure();
        kestrel.setMessage(exception.getMessage());
        return kestrel;
    }
}
