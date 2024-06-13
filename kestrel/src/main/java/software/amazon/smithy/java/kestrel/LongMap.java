/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.kestrel;

import static software.amazon.smithy.java.kestrel.KConstants.encodeVarintListLength;
import static software.amazon.smithy.java.kestrel.KestrelSerializer.ulongSize;

public final class LongMap extends NumberMap<Long> {
    @Override
    protected Long[] newArray(int len) {
        return new Long[len];
    }

    @Override
    protected int decodeValueCount(int encodedCount) {
        return KConstants.decodeVarintListLengthChecked(encodedCount);
    }

    @Override
    protected Long decode(KestrelDeserializer d) {
        return d.varL();
    }

    @Override
    protected void writeValues(KestrelSerializer s, Long[] values) {
        s.writeLongList(values);
    }

    @Override
    protected int sizeofValues(Long[] elements) {
        int n = elements.length;
        int size = ulongSize(encodeVarintListLength(n));
        for (int i = 0; i < n; i++) {
            size += KestrelSerializer.longSize(elements[i]);
        }
        return size;
    }
}
