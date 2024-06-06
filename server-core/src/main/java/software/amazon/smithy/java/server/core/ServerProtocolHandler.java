/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

public final class ServerProtocolHandler implements SyncHandler {

    @Override
    public void doBefore(Job job) {
        if (job.request().getValue().getClass() == ByteValue.class
            || job.request().getValue().getClass() == ReactiveByteValue.class) {
            job.chosenProtocol().deserializeInput(job);
        }
    }

    @Override
    public void doAfter(Job job) {
        if (job.reply().getValue().getClass() == ShapeValue.class) {
            job.chosenProtocol().serializeOutput(job);
        }
    }
}
