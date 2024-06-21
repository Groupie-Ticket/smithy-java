package software.amazon.smithy.java.runtime.http.binding;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import software.amazon.smithy.java.runtime.core.schema.Schema;
import software.amazon.smithy.java.runtime.core.serde.Codec;
import software.amazon.smithy.java.runtime.core.serde.DataStream;
import software.amazon.smithy.java.runtime.core.serde.SerializationException;
import software.amazon.smithy.java.runtime.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.runtime.core.serde.document.Document;

public class PayloadDeserializer implements ShapeDeserializer {
    private final Codec payloadCodec;
    private final DataStream body;

    public PayloadDeserializer(Codec payloadCodec, DataStream body) {
        this.payloadCodec = payloadCodec;
        this.body = body;
    }

    private byte[] resolveBodyBytes() {
        try {
            return body.asBytes().toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new SerializationException("Failed to get payload bytes", e);
        }
    }

    @Override
    public boolean readBoolean(Schema schema) {
        return payloadCodec.createDeserializer(resolveBodyBytes()).readBoolean(schema);
    }

    @Override
    public byte[] readBlob(Schema schema) {
        if (isNull()) {
            return null;
        }

        return resolveBodyBytes();
    }

    @Override
    public byte readByte(Schema schema) {
        return payloadCodec.createDeserializer(resolveBodyBytes()).readByte(schema);
    }

    @Override
    public short readShort(Schema schema) {
        return payloadCodec.createDeserializer(resolveBodyBytes()).readShort(schema);
    }

    @Override
    public int readInteger(Schema schema) {
        return payloadCodec.createDeserializer(resolveBodyBytes()).readInteger(schema);
    }

    @Override
    public long readLong(Schema schema) {
        return payloadCodec.createDeserializer(resolveBodyBytes()).readLong(schema);
    }

    @Override
    public float readFloat(Schema schema) {
        return payloadCodec.createDeserializer(resolveBodyBytes()).readFloat(schema);
    }

    @Override
    public double readDouble(Schema schema) {
        return payloadCodec.createDeserializer(resolveBodyBytes()).readDouble(schema);
    }

    @Override
    public BigInteger readBigInteger(Schema schema) {
        if (isNull()) {
            return null;
        }

        return payloadCodec.createDeserializer(resolveBodyBytes()).readBigInteger(schema);
    }

    @Override
    public BigDecimal readBigDecimal(Schema schema) {
        if (isNull()) {
            return null;
        }

        return payloadCodec.createDeserializer(resolveBodyBytes()).readBigDecimal(schema);
    }

    @Override
    public String readString(Schema schema) {
        if (isNull()) {
            return null;
        }

        try {
            return body.asString().toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new SerializationException("Failed to get payload bytes", e);
        }
    }

    @Override
    public Document readDocument() {
        if (isNull()) {
            return null;
        }

        return payloadCodec.createDeserializer(resolveBodyBytes()).readDocument();
    }

    @Override
    public Instant readTimestamp(Schema schema) {
        if (isNull()) {
            return null;
        }

        return payloadCodec.createDeserializer(resolveBodyBytes()).readTimestamp(schema);
    }

    @Override
    public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
        if (!isNull()) {
            payloadCodec.createDeserializer(resolveBodyBytes()).readStruct(schema, state, consumer);
        }
    }

    @Override
    public <T> void readList(Schema schema, T state, ListMemberConsumer<T> consumer) {
        if (!isNull()) {
            payloadCodec.createDeserializer(resolveBodyBytes()).readList(schema, state, consumer);
        }
    }

    @Override
    public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer) {
        if (!isNull()) {
            payloadCodec.createDeserializer(resolveBodyBytes()).readStringMap(schema, state, consumer);
        }
    }

    @Override
    public boolean isNull() {
        return body == null;
    }
}
