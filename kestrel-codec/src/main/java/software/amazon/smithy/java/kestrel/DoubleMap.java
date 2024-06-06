/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.kestrel;

import static software.amazon.smithy.java.kestrel.KConstants.encodeFourBListLength;
import static software.amazon.smithy.java.kestrel.KestrelSerializer.ulongSize;

public final class DoubleMap extends NumberMap<Double> {
    @Override
    protected Double[] newArray(int len) {
        return new Double[len];
    }

    @Override
    protected int decodeValueCount(int encodedCount) {
        return KConstants.decodeEightByteListLengthChecked(encodedCount);
    }

    @Override
    protected Double decode(KestrelDeserializer d) {
        return d.d8();
    }

    @Override
    protected void writeValues(KestrelSerializer s, Double[] values) {
        s.writeDoubleList(values);
    }

    @Override
    protected int sizeofValues(Double[] elements) {
        int n = elements.length;
        int size = ulongSize(encodeFourBListLength(n));
        return size + (8 * n);
    }
}
