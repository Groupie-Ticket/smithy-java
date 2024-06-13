/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.kestrel.codec;

import java.nio.ByteBuffer;
import software.amazon.smithy.java.kestrel.KestrelDeserializer;
import software.amazon.smithy.java.kestrel.KestrelSerializer;
import software.amazon.smithy.java.kestrel.KestrelStructure;
import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;

public abstract class KestrelCodec<I extends SerializableStruct, O extends SerializableStruct, KI extends KestrelStructure<I>, KO extends KestrelStructure<O>> {

    public abstract byte[] encode(O value);

    public abstract I decode(ByteBuffer buffer);

    protected final byte[] serialize(KO object) {
        KestrelSerializer s = new KestrelSerializer(object.size());
        object.encodeTo(s);
        return s.payload();
    }

    protected final I deserialize(ByteBuffer buffer, KI input) {
        KestrelDeserializer d = new KestrelDeserializer(buffer);
        input.decodeFrom(d);
        return input.convertTo();

    }

}
