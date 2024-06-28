/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.sparrowhawk.codec;

public interface SparrowhawkCodecFactory {

    String serviceName();

    SparrowhawkCodec getCodec(String operationName);
}
