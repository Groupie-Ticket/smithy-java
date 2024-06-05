/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import java.util.concurrent.CompletableFuture;

public interface SyncHandler extends Handler {

    @Override
    default CompletableFuture<Void> before(Job job) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            this.doBefore(job);
            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    default CompletableFuture<Void> after(Job job) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            this.doAfter(job);
            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    void doBefore(Job job);

    void doAfter(Job job);


}
