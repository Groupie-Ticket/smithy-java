/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.restjson.router;

class UrlEncoderException extends RuntimeException {
    public UrlEncoderException(String message) {
        super(message);
    }

    public UrlEncoderException(Throwable cause) {
        super(cause);
    }

    public UrlEncoderException(String message, Throwable cause) {
        super(message, cause);
    }
}
