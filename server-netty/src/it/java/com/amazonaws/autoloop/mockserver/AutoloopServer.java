/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.autoloop.mockserver;

import static org.reactivestreams.FlowAdapters.toFlowPublisher;
import static org.reactivestreams.FlowAdapters.toPublisher;

import com.amazon.hyperloop.streaming.model.*;
import com.amazon.hyperloop.streaming.service.Autoloop;
import com.amazon.hyperloop.streaming.service.CreateAttributeSyncStreamOperation;
import com.amazonaws.autoloop.mockserver.processing.CreateAttributeSyncStreamValidator;
import com.amazonaws.autoloop.mockserver.processing.EdgeEventProcessor;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Notification;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Flow;
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
            return CreateAttributeSyncStreamOutput.builder().event(getStream(input)).build();
        }

        private Flow.Publisher<CloudEvent> getStream(CreateAttributeSyncStreamInput input) {
            return toFlowPublisher(
                Flowable.fromPublisher(toPublisher(input.event()))
                    .observeOn(Schedulers.computation())
                    .materialize()
                    .concatMap(new CreateAttributeSyncStreamProcessor(edgeEventProcessor, input)::getFlowable)
            );
        }

        private static final class CreateAttributeSyncStreamProcessor {
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

            public Flowable<CloudEvent> getFlowable(Notification<EdgeEvent> event) {
                if (event.isOnComplete()) {
                    return Flowable.empty();
                } else if (event.isOnError()) {
                    return Flowable.error(event.getError());
                }
                try {
                    return Flowable.fromIterable(
                        List.of(edgeEventProcessor.process(event.getValue(), createAttributeSyncStreamInput))
                    );
                } catch (UnsupportedOperationException | IllegalStateException e) {
                    return Flowable.error(InternalServerException.builder().message(e.getMessage()).build());
                }
            }

        }
    }
}
