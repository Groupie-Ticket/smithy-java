package software.amazon.smithy.java.runtime.http.binding;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import software.amazon.smithy.java.runtime.core.schema.Schema;
import software.amazon.smithy.java.runtime.core.serde.SerializationException;
import software.amazon.smithy.java.runtime.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.runtime.core.serde.TimestampFormatter;
import software.amazon.smithy.java.runtime.core.serde.document.Document;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

final class HttpQueryStringDeserializer implements ShapeDeserializer {

    private final List<String> values;

    HttpQueryStringDeserializer(List<String> values) {
        this.values = values;
    }

    @Override
    public boolean readBoolean(Schema schema) {
        return "true".equals(values.get(0));
    }

    @Override
    public byte[] readBlob(Schema schema) {
        try {
            return Base64.getDecoder().decode(values.get(0).getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new SerializationException("Invalid base64");
        }
    }

    @Override
    public byte readByte(Schema schema) {
        try {
            return Byte.parseByte(values.get(0));
        } catch (NumberFormatException nfe) {
            throw new SerializationException("Invalid byte");
        }
    }

    @Override
    public short readShort(Schema schema) {
        try {
            return Short.parseShort(values.get(0));
        } catch (NumberFormatException nfe) {
            throw new SerializationException("Invalid short");
        }
    }

    @Override
    public int readInteger(Schema schema) {
        try {
            return Integer.parseInt(values.get(0));
        } catch (NumberFormatException nfe) {
            throw new SerializationException("Invalid integer");
        }
    }

    @Override
    public long readLong(Schema schema) {
        try {
            return Long.parseLong(values.get(0));
        } catch (NumberFormatException nfe) {
            throw new SerializationException("Invalid long");
        }
    }

    @Override
    public float readFloat(Schema schema) {
        try {
            return Float.parseFloat(values.get(0));
        } catch (NumberFormatException nfe) {
            throw new SerializationException("Invalid float");
        }
    }

    @Override
    public double readDouble(Schema schema) {
        try {
            return Double.parseDouble(values.get(0));
        } catch (NumberFormatException nfe) {
            throw new SerializationException("Invalid double");
        }
    }

    @Override
    public BigInteger readBigInteger(Schema schema) {
        try {
            return new BigInteger(values.get(0));
        } catch (NumberFormatException nfe) {
            throw new SerializationException("Invalid BigInteger");
        }
    }

    @Override
    public BigDecimal readBigDecimal(Schema schema) {
        try {
            return new BigDecimal(values.get(0));
        } catch (NumberFormatException nfe) {
            throw new SerializationException("Invalid BigDecimal");
        }
    }

    @Override
    public String readString(Schema schema) {
        return values.get(0);
    }

    @Override
    public Document readDocument() {
        throw new UnsupportedOperationException("Documents are not supported in HTTP query-string bindings");
    }

    @Override
    public Instant readTimestamp(Schema schema) {
        var trait = schema.getTrait(TimestampFormatTrait.class);
        TimestampFormatter formatter = trait != null
            ? TimestampFormatter.of(trait)
            : TimestampFormatter.Prelude.DATE_TIME;
        return formatter.readFromString(values.get(0), false);
    }

    @Override
    public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
        throw new UnsupportedOperationException("Structures are not supported in HTTP query-string bindings");
    }

    @Override
    public <T> void readList(Schema schema, T state, ListMemberConsumer<T> consumer) {
        for (String value : values) {
            consumer.accept(state, new HttpQueryStringDeserializer(List.of(value)));
        }
    }

    @Override
    public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer) {
        throw new UnsupportedOperationException("Maps are not supported in HTTP query-string bindings");
    }

    @Override
    public boolean isNull() {
        return values == null;
    }
}
