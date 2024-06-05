/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.core;

import java.nio.charset.StandardCharsets;
import software.amazon.smithy.java.server.Service;


public class OperationHandler implements SyncHandler {

    private final Service service;

    public OperationHandler(Service service) {
        this.service = service;
    }

    @Override
    public void doBefore(Job job) {
        service.getOperation("GetBeer").function().apply(null, null);
        job.getReply().setValue(new ByteValue("test".getBytes(StandardCharsets.UTF_8)));
        job.setDone();

    }

    @Override
    public void doAfter(Job job) {

    }
}
