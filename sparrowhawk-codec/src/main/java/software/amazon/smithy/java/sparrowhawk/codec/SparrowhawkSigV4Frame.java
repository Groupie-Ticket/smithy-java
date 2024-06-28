/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.sparrowhawk.codec;

import static software.amazon.smithy.java.sparrowhawk.KConstants.T_EIGHT;
import static software.amazon.smithy.java.sparrowhawk.KConstants.T_LIST;
import static software.amazon.smithy.java.sparrowhawk.KConstants.decodeElementCount;
import static software.amazon.smithy.java.sparrowhawk.KConstants.encodeByteListLength;
import static software.amazon.smithy.java.sparrowhawk.SparrowhawkSerializer.byteListLengthEncodedSize;
import static software.amazon.smithy.java.sparrowhawk.SparrowhawkSerializer.missingField;
import static software.amazon.smithy.java.sparrowhawk.SparrowhawkSerializer.ulongSize;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Objects;
import software.amazon.smithy.java.sparrowhawk.SparrowhawkDeserializer;
import software.amazon.smithy.java.sparrowhawk.SparrowhawkObject;
import software.amazon.smithy.java.sparrowhawk.SparrowhawkSerializer;


// Generated file, do not edit.
public final class SparrowhawkSigV4Frame implements SparrowhawkObject {
    private static final long REQUIRED_LIST_0 = 0x8L;
    private long $list_0 = REQUIRED_LIST_0;
    // list fieldSet 0 index 1
    private static final long FIELD_CHUNKSIGNATURE = 0x8L;
    private ByteBuffer chunkSignature;

    public ByteBuffer getChunkSignature() {
        return chunkSignature;
    }

    public void setChunkSignature(ByteBuffer chunkSignature) {
        if (chunkSignature == null) {
            missingField("'chunkSignature' is required");
        }
        this.chunkSignature = chunkSignature;
        this.$size = 0;
    }

    public boolean hasChunkSignature() {
        return ($list_0 & FIELD_CHUNKSIGNATURE) != 0;
    }

    // list fieldSet 0 index 2
    private static final long FIELD_CHUNK = 0x10L;
    private ByteBuffer chunk;

    public ByteBuffer getChunk() {
        return chunk;
    }

    public void setChunk(ByteBuffer chunk) {
        if (chunk == null) {
            $list_0 &= ~FIELD_CHUNK;
        } else {
            $list_0 |= FIELD_CHUNK;
        }
        this.chunk = chunk;
        this.$size = 0;
    }

    public boolean hasChunk() {
        return ($list_0 & FIELD_CHUNK) != 0;
    }

    private static final long REQUIRED_EIGHT_BYTE_0 = 0xbL;
    private long $eightByte_0 = REQUIRED_EIGHT_BYTE_0;
    // eightByte fieldSet 0 index 1
    private static final long FIELD_DATE = 0x8L;
    private Date date;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
        this.$size = 0;
    }

    public boolean hasDate() {
        return ($eightByte_0 & FIELD_DATE) != 0;
    }

    private int $size;

    public int size() {
        if ($size > 0) {
            return $size;
        }

        int size = ($list_0 == 0x0L ? 0 : (ulongSize($list_0))) + ($eightByte_0 == 0x3L
            ? 0
            : (ulongSize($eightByte_0)));
        size += sizeListFields();
        size += sizeEightByteFields();
        this.$size = size;
        return size;
    }

    private int sizeEightByteFields() {
        int size = 8;
        return size;
    }

    private int sizeListFields() {
        int size = 0;
        size += byteListLengthEncodedSize(chunkSignature.remaining());
        // Manual edit: This is just chunk.remaining() instead of byteListLengthEncodedSize(chunk.remaining()) as the
        // first byte(s) of the chunk are already a byte list length encoded size.
        if (hasChunk()) {
            size += chunk.remaining();
        }
        return size;
    }

    public void encodeTo(SparrowhawkSerializer s) {
        s.writeVarUL(encodeByteListLength(size()));
        writeEightByteFields(s);
        writeListFields(s);
    }

    private void writeEightByteFields(SparrowhawkSerializer s) {
        s.writeVarUL($eightByte_0);
        s.writeDate(date);
    }

    private void writeListFields(SparrowhawkSerializer s) {
        s.writeVarUL($list_0);
        s.writeBytes(chunkSignature);
        if (hasChunk()) {
            // Manual edit: This is writeEncodedObject instead of writeBytes
            s.writeEncodedObject(chunk);
        }
    }

    public void decodeFrom(SparrowhawkDeserializer d) {
        int size = (int) decodeElementCount(d.varUI());
        this.$size = size;
        int start = d.pos();

        while ((d.pos() - start) < size) {
            long fieldSet = d.varUL();
            int fieldSetIdx = ((fieldSet & 0b100) != 0) ? d.varUI() + 1 : 0;
            int type = (int) (fieldSet & 3);
            if (type == T_LIST) {
                if (fieldSetIdx != 0) { throw new IllegalArgumentException("unknown fieldSetIdx " + fieldSetIdx); }
                decodeListFieldSet0(d, fieldSet);
            } else if (type == T_EIGHT) {
                if (fieldSetIdx != 0) { throw new IllegalArgumentException("unknown fieldSetIdx " + fieldSetIdx); }
                decodeEightByteFieldSet0(d, fieldSet);
            } else {
                throw new RuntimeException("Unexpected field set type: " + type);
            }
        }
    }

    private void decodeEightByteFieldSet0(SparrowhawkDeserializer d, long fieldSet) {
        SparrowhawkDeserializer.checkFields(fieldSet, REQUIRED_EIGHT_BYTE_0, "eight-byte");
        this.$eightByte_0 = fieldSet;
        this.date = d.date();
    }

    private void decodeListFieldSet0(SparrowhawkDeserializer d, long fieldSet) {
        SparrowhawkDeserializer.checkFields(fieldSet, REQUIRED_LIST_0, "lists");
        this.$list_0 = fieldSet;
        {
            this.chunkSignature = d.bytes();
        }
        if (hasChunk()) {
            // Manual edit: This is object() instead of bytes()
            this.chunk = d.object();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SparrowhawkSigV4Frame)) return false;
        SparrowhawkSigV4Frame o = (SparrowhawkSigV4Frame) other;
        if (!Objects.equals(getDate(), o.getDate())) {
            return false;
        }
        if (!Objects.equals(getChunkSignature(), o.getChunkSignature())) {
            return false;
        }
        if (!Objects.equals(getChunk(), o.getChunk())) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException("no hashing");
    }
}
