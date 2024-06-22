/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.http.binding;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import software.amazon.smithy.java.runtime.core.schema.Schema;
import software.amazon.smithy.java.runtime.core.serde.SerializationException;
import software.amazon.smithy.java.runtime.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.runtime.core.serde.TimestampFormatter;
import software.amazon.smithy.java.runtime.core.serde.document.Document;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

final class HttpHeaderDeserializer implements ShapeDeserializer {

    private final String value;

    HttpHeaderDeserializer(String value) {
        this.value = value;
    }

    @Override
    public boolean readBoolean(Schema schema) {
        return switch (value) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new SerializationException("Invalid boolean");
        };
    }

    @Override
    public byte[] readBlob(Schema schema) {
        try {
            return Base64.getDecoder().decode(value.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new SerializationException("Invalid base64");
        }
    }

    @Override
    public byte readByte(Schema schema) {
        try {
            return Byte.parseByte(value);
        } catch (NumberFormatException e) {
            throw new SerializationException("Invalid byte");
        }
    }

    @Override
    public short readShort(Schema schema) {
        try {
            return Short.parseShort(value);
        } catch (NumberFormatException e) {
            throw new SerializationException("Invalid short");
        }
    }

    @Override
    public int readInteger(Schema schema) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new SerializationException("Invalid integer");
        }
    }

    @Override
    public long readLong(Schema schema) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new SerializationException("Invalid long");
        }
    }

    @Override
    public float readFloat(Schema schema) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw new SerializationException("Invalid float");
        }
    }

    @Override
    public double readDouble(Schema schema) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new SerializationException("Invalid double");
        }
    }

    @Override
    public BigInteger readBigInteger(Schema schema) {
        try {
            return new BigInteger(value);
        } catch (NumberFormatException e) {
            throw new SerializationException("Invalid BigInteger");
        }
    }

    @Override
    public BigDecimal readBigDecimal(Schema schema) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new SerializationException("Invalid BigDecimal");
        }
    }

    @Override
    public String readString(Schema schema) {
        if (schema.hasTrait(MediaTypeTrait.class)) {
            try {
                return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                throw new SerializationException("Invalid base64");
            }
        }
        return value;
    }

    @Override
    public Document readDocument() {
        throw new UnsupportedOperationException("Documents are not supported in HTTP header bindings");
    }

    @Override
    public Instant readTimestamp(Schema schema) {
        var trait = schema.getTrait(TimestampFormatTrait.class);
        TimestampFormatter formatter = trait != null
            ? TimestampFormatter.of(trait)
            : TimestampFormatter.Prelude.HTTP_DATE;
        return formatter.readFromString(value, false); // headers always are strings.
    }

    @Override
    public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> structMemberConsumer) {
        throw new UnsupportedOperationException("Structures are not supported in HTTP header bindings");
    }

    @Override
    public <T> void readList(Schema schema, T state, ListMemberConsumer<T> listMemberConsumer) {
        throw new UnsupportedOperationException("List header support not yet implemented");
    }

    @Override
    public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> mapMemberConsumer) {
        throw new UnsupportedOperationException("List map support not yet implemented");
    }

    @Override
    public boolean isNull() {
        return value == null;
    }
}
