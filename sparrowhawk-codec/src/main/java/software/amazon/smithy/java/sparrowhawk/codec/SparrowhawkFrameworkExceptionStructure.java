/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.sparrowhawk.codec;

import static java.nio.charset.StandardCharsets.UTF_8;
import static software.amazon.smithy.java.sparrowhawk.KConstants.*;
import static software.amazon.smithy.java.sparrowhawk.SparrowhawkSerializer.byteListLengthEncodedSize;
import static software.amazon.smithy.java.sparrowhawk.SparrowhawkSerializer.ulongSize;

import software.amazon.smithy.java.sparrowhawk.SparrowhawkDeserializer;
import software.amazon.smithy.java.sparrowhawk.SparrowhawkObject;
import software.amazon.smithy.java.sparrowhawk.SparrowhawkSerializer;

public class SparrowhawkFrameworkExceptionStructure implements SparrowhawkObject {

    private static final long REQUIRED_LIST_0 = 0x0L;
    private long $list_0 = REQUIRED_LIST_0;
    // list fieldSet 0 index 1
    private static final long FIELD_MESSAGE = 0x8L;
    private Object message;

    public String getMessage() {
        if (message == null) {
            return null;
        }
        if (message instanceof String) {
            return (String) message;
        }
        String s = new String((byte[]) message, UTF_8);
        this.message = s;
        return s;
    }

    public void setMessage(String message) {
        if (message == null) {
            $list_0 &= ~FIELD_MESSAGE;
        } else {
            $list_0 |= FIELD_MESSAGE;
        }
        this.message = message;
        this.$size = 0;
    }

    public boolean hasMessage() {
        return ($list_0 & FIELD_MESSAGE) != 0;
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
        if (hasMessage()) {
            size += $messageLen();
        }
        return size;
    }

    private int $messageLen() {
        Object field = message;
        int size;
        if (field.getClass() == byte[].class) {
            size = ((byte[]) field).length;
        } else {
            byte[] bytes = ((String) field).getBytes(UTF_8);
            this.message = bytes;
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
            if (hasMessage()) {
                s.writeBytes(message);
            }
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
                if (fieldSetIdx != 0) {
                    throw new IllegalArgumentException("unknown fieldSetIdx " + fieldSetIdx);
                }
                decodeListFieldSet0(d, fieldSet);
            } else {
                throw new RuntimeException("Unexpected field set type: " + type);
            }
        }
    }

    private void decodeListFieldSet0(SparrowhawkDeserializer d, long fieldSet) {
        this.$list_0 = fieldSet;
        if (hasMessage()) {
            this.message = d.string();
        }
    }
}
