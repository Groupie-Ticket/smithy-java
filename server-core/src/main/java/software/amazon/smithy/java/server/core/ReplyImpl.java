/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import software.amazon.smithy.java.runtime.core.Context;

public final class ReplyImpl implements Reply {

    private Value value;

    @Override
    public Context getContext() {
        return null;
    }

    @Override
    public <T extends Value> void setValue(T value) {
        this.value = value;
    }

    @Override
    public <T extends Value> T getValue() {
        return (T) value;
    }
}
