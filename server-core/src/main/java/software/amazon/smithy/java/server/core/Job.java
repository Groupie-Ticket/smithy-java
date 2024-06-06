/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import java.util.Optional;
import software.amazon.smithy.java.runtime.core.Context;
import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;
import software.amazon.smithy.java.server.Operation;

public sealed interface Job permits JobImpl {

    Request request();

    Reply reply();

    Context context();

    boolean isDone();

    void setDone();

    Optional<Throwable> getFailure();

    void setFailure(Throwable t);

    Operation<? extends SerializableStruct, ? extends SerializableStruct> operation();

    ServerProtocol chosenProtocol();

}
