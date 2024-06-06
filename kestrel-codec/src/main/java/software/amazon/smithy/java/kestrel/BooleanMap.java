/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.kestrel;

import static software.amazon.smithy.java.kestrel.KConstants.encodeVarintListLength;
import static software.amazon.smithy.java.kestrel.KestrelSerializer.ulongSize;

public final class BooleanMap extends NumberMap<Boolean> {
    @Override
    protected Boolean[] newArray(int len) {
        return new Boolean[len];
    }

    @Override
    protected int decodeValueCount(int encodedCount) {
        return KConstants.decodeVarintListLengthChecked(encodedCount);
    }

    @Override
    protected Boolean decode(KestrelDeserializer d) {
        return d.bool();
    }

    @Override
    protected void writeValues(KestrelSerializer s, Boolean[] values) {
        s.writeBooleanList(values);
    }

    @Override
    protected int sizeofValues(Boolean[] elements) {
        int n = elements.length;
        int size = ulongSize(encodeVarintListLength(n));
        return size + elements.length;
    }
}
