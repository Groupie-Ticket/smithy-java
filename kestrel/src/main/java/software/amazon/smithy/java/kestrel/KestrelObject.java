/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.kestrel;

public interface KestrelObject {
    void decodeFrom(KestrelDeserializer d);

    void encodeTo(KestrelSerializer s);

    int size();
}
