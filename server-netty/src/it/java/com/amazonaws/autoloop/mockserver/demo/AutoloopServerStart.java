/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.autoloop.mockserver.demo;

import com.amazonaws.autoloop.mockserver.AutoloopServer;
import com.amazonaws.autoloop.mockserver.processing.CreateAttributeSyncStreamValidator;
import com.amazonaws.autoloop.mockserver.processing.EdgeEventProcessor;
import java.net.URI;
import org.junit.jupiter.api.Test;

class AutoloopServerStart {

    @Test
    void start() {
        CreateAttributeSyncStreamValidator createAttributeSyncStreamValidator = new CreateAttributeSyncStreamValidatorImpl();
        EdgeEventProcessor edgeEventProcessor = new EdgeEventProcessorImpl();
        URI endpoint = URI.create("http://localhost:8080");

        AutoloopServer server = new AutoloopServer(edgeEventProcessor, createAttributeSyncStreamValidator, endpoint);
        server.start();
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            server.stop();
        }
    }
}
