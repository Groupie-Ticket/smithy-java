/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import java.util.concurrent.CompletableFuture;

public final class ServerProtocolHandler implements Handler {

    @Override
    public CompletableFuture<Void> before(Job job) {
        if (job.request().getValue().getClass() == ByteValue.class
            || job.request().getValue().getClass() == ReactiveByteValue.class) {
            try {
                return job.chosenProtocol().deserializeInput(job);
            } catch (Throwable t) {
                return CompletableFuture.failedFuture(t);
            }
        }

        return couldNotClaim();
    }

    private static CompletableFuture<Void> couldNotClaim() {
        return CompletableFuture.failedFuture(new RuntimeException("Nothing claimed the job"));
    }

    @Override
    public CompletableFuture<Void> after(Job job) {
        // null check
        if (job.reply().getValue() instanceof ShapeValue || job.getFailure().isPresent()) {
            try {
                return job.chosenProtocol().serializeOutput(job);
            } catch (Throwable t) {
                return CompletableFuture.failedFuture(t);
            }
        }
        return CompletableFuture.completedFuture(null);
    }
}
