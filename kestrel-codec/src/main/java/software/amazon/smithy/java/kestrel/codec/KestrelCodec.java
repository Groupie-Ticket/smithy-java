/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.kestrel.codec;

import java.nio.ByteBuffer;
import software.amazon.smithy.java.kestrel.KestrelDeserializer;
import software.amazon.smithy.java.kestrel.KestrelObject;
import software.amazon.smithy.java.kestrel.KestrelSerializer;
import software.amazon.smithy.java.runtime.core.schema.Schema;
import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;

public abstract class KestrelCodec<I extends SerializableStruct, O extends SerializableStruct, KI extends KestrelStructure<I>, KO extends KestrelStructure<O>> {

    public abstract byte[] encode(O value);

    public abstract I decode(ByteBuffer buffer);

    public final byte[] encodeException(Schema schema, Throwable exception) {
        var converted = convertException(exception);
        var kestrelWrapper = new KestrelExceptionWrapper();
        kestrelWrapper.setSerializedError(ByteBuffer.wrap(serializeK(converted)));
        kestrelWrapper.setType(schema.id().toString());
        return serializeK(kestrelWrapper);
    }

    protected abstract KestrelObject convertException(Throwable exception);

    protected final byte[] serialize(KO object) {
        return serializeK(object);
    }

    private byte[] serializeK(KestrelObject object) {
        KestrelSerializer s = new KestrelSerializer(object.size());
        object.encodeTo(s);
        return s.payload();
    }

    protected final I deserialize(ByteBuffer buffer, KI input) {
        KestrelDeserializer d = new KestrelDeserializer(buffer);
        input.decodeFrom(d);
        return input.convertTo();
    }

    protected KestrelObject createSynthetic(Throwable exception) {
        var kestrel = new KestrelFrameworkExceptionStructure();
        kestrel.setMessage(exception.getMessage());
        return kestrel;
    }

}
