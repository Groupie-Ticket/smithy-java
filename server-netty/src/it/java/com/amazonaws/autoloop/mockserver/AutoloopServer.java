/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.autoloop.mockserver;

import com.amazon.hyperloop.streaming.model.CloudEvent;
import com.amazon.hyperloop.streaming.model.CreateAttributeSyncStreamInput;
import com.amazon.hyperloop.streaming.model.CreateAttributeSyncStreamOutput;
import com.amazon.hyperloop.streaming.model.EdgeEvent;
import com.amazon.hyperloop.streaming.model.InternalServerException;
import com.amazon.hyperloop.streaming.service.Autoloop;
import com.amazon.hyperloop.streaming.service.CreateAttributeSyncStreamOperation;
import com.amazonaws.autoloop.mockserver.processing.CreateAttributeSyncStreamValidator;
import com.amazonaws.autoloop.mockserver.processing.EdgeEventProcessor;
import java.net.URI;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.smithy.java.server.RequestContext;
import software.amazon.smithy.java.server.Server;

public class AutoloopServer {


    private final Server server;

    public AutoloopServer(
        EdgeEventProcessor edgeEventProcessor,
        CreateAttributeSyncStreamValidator createAttributeSyncStreamValidator,
        URI uri
    ) {
        CreateAttributeSyncStreamImpl createAttributeSyncStream = new CreateAttributeSyncStreamImpl(
            edgeEventProcessor,
            createAttributeSyncStreamValidator
        );

        server = Server.builder(uri)
            .addService(
                Autoloop.builder()
                    .addCreateAttributeSyncStreamOperation(createAttributeSyncStream)
                    .build()
            )
            .build();
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop();
    }


    private static final class CreateAttributeSyncStreamImpl implements CreateAttributeSyncStreamOperation {
        private final EdgeEventProcessor edgeEventProcessor;
        private final CreateAttributeSyncStreamValidator createAttributeSyncStreamValidator;

        CreateAttributeSyncStreamImpl(
            EdgeEventProcessor edgeEventProcessor,
            CreateAttributeSyncStreamValidator createAttributeSyncStreamValidator
        ) {
            this.edgeEventProcessor = edgeEventProcessor;
            this.createAttributeSyncStreamValidator = createAttributeSyncStreamValidator;
        }

        @Override
        public CreateAttributeSyncStreamOutput createAttributeSyncStream(
            CreateAttributeSyncStreamInput input,
            RequestContext context
        ) {
            // validate CreateAttributeSyncStreamInput
            createAttributeSyncStreamValidator.validate(input);

            // setup eventhandling chain
            CreateAttributeSyncStreamProcessor processor = new CreateAttributeSyncStreamProcessor(
                edgeEventProcessor,
                input
            );
            input.event().subscribe(processor);
            return CreateAttributeSyncStreamOutput.builder().event(processor).build();
        }

        private static final class CreateAttributeSyncStreamProcessor extends SubmissionPublisher<CloudEvent> implements
            Flow.Subscriber<EdgeEvent> {
            private final AtomicReference<Flow.Subscription> upstream = new AtomicReference<>();
            private final EdgeEventProcessor edgeEventProcessor;
            private final CreateAttributeSyncStreamInput createAttributeSyncStreamInput;

            public CreateAttributeSyncStreamProcessor(
                EdgeEventProcessor edgeEventProcessor,
                CreateAttributeSyncStreamInput createAttributeSyncStreamInput
            ) {
                this.edgeEventProcessor = edgeEventProcessor;
                this.createAttributeSyncStreamInput = createAttributeSyncStreamInput;
            }

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                if (!upstream.compareAndSet(null, subscription)) {
                    throw new IllegalStateException();
                }
                subscription.request(1);
            }

            @Override
            public void onNext(EdgeEvent item) {
                CloudEvent result = null;
                try {
                    result = edgeEventProcessor.process(item, createAttributeSyncStreamInput);
                    submit(result);
                } catch (UnsupportedOperationException | IllegalStateException ex) {
                    throw InternalServerException.builder().message(ex.getMessage()).build();
                }

                upstream.get().request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                closeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                super.close();
            }
        }
    }
}
