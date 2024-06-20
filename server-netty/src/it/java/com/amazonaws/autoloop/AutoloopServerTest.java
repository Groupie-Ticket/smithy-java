/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.autoloop;


import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.amazon.hyperloop.streaming.kestrel.KestrelCreateAttributeSyncStreamInput;
import com.amazon.hyperloop.streaming.model.CloudEvent;
import com.amazon.hyperloop.streaming.model.CreateAttributeSyncStreamInput;
import com.amazon.hyperloop.streaming.model.CreateAttributeSyncStreamOutput;
import com.amazon.hyperloop.streaming.model.EdgeEvent;
import com.amazon.hyperloop.streaming.model.ValidationException;
import com.amazon.hyperloop.streaming.service.Autoloop;
import com.amazon.hyperloop.streaming.service.CreateAttributeSyncStreamOperation;
import java.net.URI;
import java.util.Base64;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.java.kestrel.KestrelSerializer;
import software.amazon.smithy.java.server.RequestContext;
import software.amazon.smithy.java.server.Server;

class AutoloopServerTest {


    private static class EdgeEventProcessor {

        CloudEvent process(EdgeEvent edgeEvent) {
            // switch on edge event tpyes
            return null;
        }
    }


    private static class CreateAttributeSyncStreamValidator {
        void validate(CreateAttributeSyncStreamInput input) {
            if (input.dataSyncEngineIdentifier() == null) {
                throw ValidationException.builder().message("DSE shall not be null").build();
            }
        }
    }

    private static final class CreateAttributeSyncStream implements CreateAttributeSyncStreamOperation {
        private final EdgeEventProcessor edgeEventProcessor;
        private final CreateAttributeSyncStreamValidator createAttributeSyncStreamValidator;

        CreateAttributeSyncStream(
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
            // handle issues on API level
            createAttributeSyncStreamValidator.validate(input);

            // setup eventhandling chain
            CreateAttributeSyncStreamProcessor processor = new CreateAttributeSyncStreamProcessor(edgeEventProcessor);
            input.event().subscribe(processor);
            return CreateAttributeSyncStreamOutput.builder().event(processor).build();
        }

        private static final class CreateAttributeSyncStreamProcessor extends SubmissionPublisher<CloudEvent> implements
            Flow.Subscriber<EdgeEvent> {
            private final AtomicReference<Flow.Subscription> upstream = new AtomicReference<>();
            private final EdgeEventProcessor edgeEventProcessor;

            public CreateAttributeSyncStreamProcessor(EdgeEventProcessor edgeEventProcessor) {
                this.edgeEventProcessor = edgeEventProcessor;
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
                submit(edgeEventProcessor.process(item));
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

    @Test
    void testAutoloopBuilder() {
        CreateAttributeSyncStream createAttributeSyncStream = new CreateAttributeSyncStream(
            new EdgeEventProcessor(),
            new CreateAttributeSyncStreamValidator()
        );
        Autoloop.BuildStage builder = Autoloop.builder()
            .addCreateAttributeSyncStreamOperation(createAttributeSyncStream);
        Autoloop service = builder.build();
        assertNotNull(service);
    }

    @Test
    void testServer() {
        CreateAttributeSyncStreamValidator createAttributeSyncStreamValidator = new CreateAttributeSyncStreamValidator();
        EdgeEventProcessor edgeEventProcessor = new EdgeEventProcessor();
        CreateAttributeSyncStream createAttributeSyncStream = new CreateAttributeSyncStream(
            edgeEventProcessor,
            createAttributeSyncStreamValidator
        );
        var server = Server.builder(URI.create("http://localhost:8080"))
            .addService(
                Autoloop.builder()
                    .addCreateAttributeSyncStreamOperation(createAttributeSyncStream)
                    .build()
            )
            .build();
        server.start();
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            server.stop();
        }
    }

    @Test
    void testSerialization() {

        //just outputs Kestrel serialized message
        SubmissionPublisher<EdgeEvent> publisher = new SubmissionPublisher<>();

        CreateAttributeSyncStreamInput createAttributeSyncStreamInput = CreateAttributeSyncStreamInput.builder()
            .dataSyncEngineIdentifier("dse")
            .objectId("obj1")
            .event(publisher)
            .build();
        KestrelCreateAttributeSyncStreamInput k = KestrelCreateAttributeSyncStreamInput.convertFrom(
            createAttributeSyncStreamInput
        );
        KestrelSerializer serializer = new KestrelSerializer(k.size());
        k.encodeTo(serializer);
        System.out.println(Base64.getEncoder().encodeToString(serializer.payload()));
    }
}
