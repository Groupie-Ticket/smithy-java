/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core.serde;

public class NoInitialEventException extends RuntimeException {

    public NoInitialEventException() {
    }

    public NoInitialEventException(String message) {
        super(message);
    }

    public NoInitialEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
