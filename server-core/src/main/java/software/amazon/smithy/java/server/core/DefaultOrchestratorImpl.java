/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.smithy.java.server.Service;

public class DefaultOrchestratorImpl implements Orchestrator {

    public DefaultOrchestratorImpl(List<Service> service) {

    }

    @Override
    public CompletableFuture<Job> enqueue(Job job) {
        job.getReply().setValue(new ByteValue("hello".getBytes(StandardCharsets.UTF_8)));
        return CompletableFuture.completedFuture(job);
    }
}
