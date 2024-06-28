/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.sparrowhawk.codec;


import static java.nio.charset.StandardCharsets.UTF_8;
import static software.amazon.smithy.java.sparrowhawk.KConstants.*;
import static software.amazon.smithy.java.sparrowhawk.SparrowhawkSerializer.*;

import java.nio.ByteBuffer;
import software.amazon.smithy.java.sparrowhawk.SparrowhawkDeserializer;
import software.amazon.smithy.java.sparrowhawk.SparrowhawkObject;
import software.amazon.smithy.java.sparrowhawk.SparrowhawkSerializer;


public final class SparrowhawkExceptionWrapper implements SparrowhawkObject {
    private static final long REQUIRED_LIST_0 = 0x18L;
    private long $list_0 = REQUIRED_LIST_0;
    // list fieldSet 0 index 1
    private static final long FIELD_TYPE = 0x8L;
    private Object type;

    public String getType() {
        if (type == null) {
            return null;
        }
        if (type instanceof String) {
            return (String) type;
        }
        String s = new String((byte[]) type, UTF_8);
        this.type = s;
        return s;
    }

    public void setType(String type) {
        if (type == null) {
            missingField("'type' is required");
        }
        this.type = type;
        this.$size = 0;
    }

    public boolean hasType() {
        return ($list_0 & FIELD_TYPE) != 0;
    }

    // list fieldSet 0 index 2
    private static final long FIELD_SERIALIZEDERROR = 0x10L;
    private ByteBuffer serializedError;

    public ByteBuffer getSerializedError() {
        return serializedError;
    }

    public void setSerializedError(ByteBuffer serializedError) {
        if (serializedError == null) {
            missingField("'serializedError' is required");
        }
        this.serializedError = serializedError;
        this.$size = 0;
    }

    public boolean hasSerializedError() {
        return ($list_0 & FIELD_SERIALIZEDERROR) != 0;
    }

    private int $size;

    public int size() {
        if ($size > 0) {
            return $size;
        }

        int size = ($list_0 == 0x0L ? 0 : (ulongSize($list_0)));
        size += sizeListFields();
        this.$size = size;
        return size;
    }

    private int sizeListFields() {
        int size = 0;
        size += $typeLen();
        size += serializedError.remaining();
        return size;
    }

    private int $typeLen() {
        Object field = type;
        if (field == null) {
            missingField("Required field 'type' is missing");
        }

        int size;
        if (field.getClass() == byte[].class) {
            size = ((byte[]) field).length;
        } else {
            byte[] bytes = ((String) field).getBytes(UTF_8);
            this.type = bytes;
            size = bytes.length;
        }

        return byteListLengthEncodedSize(size);
    }

    public void encodeTo(SparrowhawkSerializer s) {
        s.writeVarUL(encodeByteListLength(size()));
        writeListFields(s);
    }

    private void writeListFields(SparrowhawkSerializer s) {
        if ($list_0 != 0x0L) {
            s.writeVarUL($list_0);
            s.writeBytes(type);
            s.writeEncodedObject(serializedError);
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
            } else {
                throw new RuntimeException("Unexpected field set type: " + type);
            }
        }
    }

    private void decodeListFieldSet0(SparrowhawkDeserializer d, long fieldSet) {
        SparrowhawkDeserializer.checkFields(fieldSet, REQUIRED_LIST_0, "lists");
        this.$list_0 = fieldSet;
        {
            this.type = d.string();
        }
        {
            this.serializedError = d.object();
        }
    }
}
