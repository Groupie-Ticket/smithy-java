/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.netty;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ExecutionException;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import smithy.java.codegen.server.test.model.Beer;
import smithy.java.codegen.server.test.model.EchoInput;
import smithy.java.codegen.server.test.model.EchoOutput;
import smithy.java.codegen.server.test.model.GetBeerInput;
import smithy.java.codegen.server.test.model.GetBeerOutput;
import smithy.java.codegen.server.test.model.HashFileInput;
import smithy.java.codegen.server.test.model.HashFileOutput;
import smithy.java.codegen.server.test.model.ZipFileInput;
import smithy.java.codegen.server.test.model.ZipFileOutput;
import smithy.java.codegen.server.test.service.EchoOperation;
import smithy.java.codegen.server.test.service.GetBeerOperation;
import smithy.java.codegen.server.test.service.HashFileOperation;
import smithy.java.codegen.server.test.service.TestService;
import smithy.java.codegen.server.test.service.ZipFileOperation;
import software.amazon.smithy.java.runtime.core.serde.DataStream;
import software.amazon.smithy.java.server.RequestContext;
import software.amazon.smithy.java.server.Server;

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
            System.out.println("Beer invoked");
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


    @Test
    void testServer() throws InterruptedException {
        var server = Server.builder(URI.create("http://localhost:8080"))
            .addService(
                TestService.builder()
                    .addEchoOperation(new EchoOperationImpl())
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
