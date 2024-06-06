/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.netty;

import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import smithy.java.codegen.server.test.model.*;
import smithy.java.codegen.server.test.service.EchoOperation;
import smithy.java.codegen.server.test.service.GetBeerOperation;
import smithy.java.codegen.server.test.service.HashFileOperation;
import smithy.java.codegen.server.test.service.TestService;
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


    @Test
    void testServer() throws InterruptedException {
        var server = Server.builder(URI.create("http://localhost:8080"))
            .addService(
                TestService.builder()
                    .addEchoOperation(new EchoOperationImpl())
                    .addGetBeerOperation(new GetBeer())
                    .addHashFileOperation(new HashFile())
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
