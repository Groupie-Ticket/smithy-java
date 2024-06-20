/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.netty;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Notification;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.junit.jupiter.api.Test;
import smithy.java.codegen.server.test.model.Beer;
import smithy.java.codegen.server.test.model.BuzzEvent;
import smithy.java.codegen.server.test.model.DependencyException;
import smithy.java.codegen.server.test.model.EchoInput;
import smithy.java.codegen.server.test.model.EchoOutput;
import smithy.java.codegen.server.test.model.FizzBuzzInput;
import smithy.java.codegen.server.test.model.FizzBuzzOutput;
import smithy.java.codegen.server.test.model.FizzBuzzStream;
import smithy.java.codegen.server.test.model.FizzEvent;
import smithy.java.codegen.server.test.model.GetBeerInput;
import smithy.java.codegen.server.test.model.GetBeerOutput;
import smithy.java.codegen.server.test.model.HashFileInput;
import smithy.java.codegen.server.test.model.HashFileOutput;
import smithy.java.codegen.server.test.model.NegativeNumberException;
import smithy.java.codegen.server.test.model.NoSuchBeerException;
import smithy.java.codegen.server.test.model.ValueStream;
import smithy.java.codegen.server.test.model.ZipFileInput;
import smithy.java.codegen.server.test.model.ZipFileOutput;
import smithy.java.codegen.server.test.service.EchoOperation;
import smithy.java.codegen.server.test.service.FizzBuzzOperation;
import smithy.java.codegen.server.test.service.GetBeerOperation;
import smithy.java.codegen.server.test.service.HashFileOperation;
import smithy.java.codegen.server.test.service.TestService;
import smithy.java.codegen.server.test.service.ZipFileOperation;
import software.amazon.smithy.java.runtime.core.serde.DataStream;
import software.amazon.smithy.java.server.RequestContext;
import software.amazon.smithy.java.server.Server;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.zip.GZIPOutputStream;

import static org.reactivestreams.FlowAdapters.toFlowPublisher;
import static org.reactivestreams.FlowAdapters.toPublisher;

class NettyServerTest {

    private static final class EchoOperationImpl implements EchoOperation {

        @Override
        public EchoOutput echo(EchoInput input, RequestContext context) {
            return null;
        }
    }

    private static final class GetBeer implements GetBeerOperation {

        @Override
        public GetBeerOutput getBeer(GetBeerInput input, RequestContext context) {
            if (input.id() > 5 && input.id() < 10) {
                throw NoSuchBeerException.builder().message("BeerId is greater than 5").build();
            } else if (input.id() >= 10 && input.id() < 15) {
                throw DependencyException.builder().message("BeerId is greater than 10").build();
            } else if (input.id() >= 15) {
                throw new RuntimeException("BeerId is greater than 15");
            }
            return GetBeerOutput.builder().value(Beer.builder().name("Test Beer").build()).beerId(input.id()).build();
        }
    }

    private static final class HashFile implements HashFileOperation {

        @Override
        public HashFileOutput hashFile(HashFileInput input, RequestContext context) {
            try {
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                byte[] buf = new byte[1024];
                try (var inputStream = input.payload().asInputStream().toCompletableFuture().get()) {
                    int read;
                    while ((read = inputStream.read(buf)) != -1) {
                        sha.update(buf, 0, read);
                    }
                    return HashFileOutput.builder().hashcode(HexFormat.of().formatHex(sha.digest())).build();
                }
            } catch (NoSuchAlgorithmException | IOException | InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class ZipFile implements ZipFileOperation {

        @Override
        public ZipFileOutput zipFile(ZipFileInput input, RequestContext context) {
            return ZipFileOutput.builder()
                .payload(DataStream.ofOutputStream(os -> {
                    try (
                        GZIPOutputStream gzos = new GZIPOutputStream(os); InputStream is = input.payload()
                            .asInputStream()
                            .toCompletableFuture()
                            .get()
                    ) {
                        byte[] buffer = new byte[1024];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            gzos.write(buffer, 0, read);
                        }
                    } catch (IOException | InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }))
                .build();
        }
    }

    private static final class FizzBuzz implements FizzBuzzOperation {

        @Override
        public FizzBuzzOutput fizzBuzz(FizzBuzzInput input, RequestContext context) {
            return FizzBuzzOutput.builder().stream(getStream(input.stream())).build();
        }

        private static Flow.Publisher<FizzBuzzStream> getStream(Flow.Publisher<ValueStream> publisher) {
            return toFlowPublisher(Flowable.fromPublisher(toPublisher(publisher))
                .materialize()
                .concatMap(FizzBuzz::getFizzyBuzzyFlowable));
        }

        private static Flowable<FizzBuzzStream> getFizzyBuzzyFlowable(Notification<ValueStream> event) {
            if (event.isOnComplete()) {
                return Flowable.empty();
            } else if (event.isOnError()) {
                return Flowable.error(event.getError());
            }

            long value = event.getValue().Value().value();
            if (value < 0) {
                return Flowable.error(NegativeNumberException.builder().message(Long.toString(value)).build());
            }

            List<FizzBuzzStream> eventsToSend = new ArrayList<>(2);
            if (value % 3 == 0) {
                eventsToSend.add(FizzBuzzStream.builder()
                    .fizz(FizzEvent.builder()
                        .value(value)
                        .build())
                    .build());
            }

            if (value % 5 == 0) {
                eventsToSend.add(FizzBuzzStream.builder()
                    .buzz(BuzzEvent.builder()
                        .value(value)
                        .build())
                    .build());
            }

            return Flowable.fromIterable(eventsToSend);
        }
    }

    @Test
    void testServer() {
        var server = Server.builder(URI.create("http://localhost:8080"))
            .addService(
                TestService.builder()
                    .addEchoOperation(new EchoOperationImpl())
                    .addFizzBuzzOperation(new FizzBuzz())
                    .addGetBeerOperation(new GetBeer())
                    .addHashFileOperation(new HashFile())
                    .addZipFileOperation(new ZipFile())
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

}
