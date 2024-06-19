/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.core.serde;

public final class NoInitialEventException extends RuntimeException {
    public NoInitialEventException() {
        super(null, null, false, false);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
