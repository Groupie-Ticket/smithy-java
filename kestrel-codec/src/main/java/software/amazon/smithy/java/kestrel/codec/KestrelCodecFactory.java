/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.kestrel.codec;

public interface KestrelCodecFactory {

    String serviceName();

    KestrelCodec getCodec(String operationName);
}
