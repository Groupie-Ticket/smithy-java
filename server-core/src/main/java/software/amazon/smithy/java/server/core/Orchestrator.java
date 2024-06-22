/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public sealed interface Orchestrator extends Executor permits DefaultOrchestratorImpl {
    CompletableFuture<Job> enqueue(Job job);
}
