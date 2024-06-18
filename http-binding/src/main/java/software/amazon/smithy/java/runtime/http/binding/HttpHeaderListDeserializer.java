/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.http.binding;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import software.amazon.smithy.java.runtime.core.schema.Schema;
import software.amazon.smithy.java.runtime.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.runtime.core.serde.document.Document;

final class HttpHeaderListDeserializer implements ShapeDeserializer {

    private final List<String> values;

    HttpHeaderListDeserializer(List<String> values) {
        this.values = values;
    }

    @Override
    public boolean readBoolean(Schema schema) {
        throw new UnsupportedOperationException("List header deserialization not supported for " + schema);
    }

    @Override
    public byte[] readBlob(Schema schema) {
        throw new UnsupportedOperationException("List header deserialization not supported for " + schema);
    }

    @Override
    public byte readByte(Schema schema) {
        throw new UnsupportedOperationException("List header deserialization not supported for " + schema);
    }

    @Override
    public short readShort(Schema schema) {
        throw new UnsupportedOperationException("List header deserialization not supported for " + schema);
    }

    @Override
    public int readInteger(Schema schema) {
        throw new UnsupportedOperationException("List header deserialization not supported for " + schema);
    }

    @Override
    public long readLong(Schema schema) {
        throw new UnsupportedOperationException("List header deserialization not supported for " + schema);
    }

    @Override
    public float readFloat(Schema schema) {
        throw new UnsupportedOperationException("List header deserialization not supported for " + schema);
    }

    @Override
    public double readDouble(Schema schema) {
        throw new UnsupportedOperationException("List header deserialization not supported for " + schema);
    }

    @Override
    public BigInteger readBigInteger(Schema schema) {
        throw new UnsupportedOperationException("List header deserialization not supported for " + schema);
    }

    @Override
    public BigDecimal readBigDecimal(Schema schema) {
        throw new UnsupportedOperationException("List header deserialization not supported for " + schema);
    }

    @Override
    public String readString(Schema schema) {
        throw new UnsupportedOperationException("List header deserialization not supported for " + schema);
    }

    @Override
    public Document readDocument() {
        throw new UnsupportedOperationException("Documents are not supported in HTTP header bindings");
    }

    @Override
    public Instant readTimestamp(Schema schema) {
        throw new UnsupportedOperationException("List header deserialization not supported for " + schema);
    }

    @Override
    public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> structMemberConsumer) {
        throw new UnsupportedOperationException("Structures are not supported in HTTP header bindings");
    }

    @Override
    public <T> void readList(Schema schema, T state, ListMemberConsumer<T> listMemberConsumer) {
        for (String value : values) {
            listMemberConsumer.accept(state, new HttpHeaderDeserializer(value));
        }
    }

    @Override
    public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> mapMemberConsumer) {
        throw new UnsupportedOperationException("List map support not yet implemented");
    }

    @Override
    public boolean isNull() {
        return values == null;
    }
}
