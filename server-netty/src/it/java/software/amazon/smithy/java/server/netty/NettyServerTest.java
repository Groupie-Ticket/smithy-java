/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.netty;

import java.net.URI;
import org.junit.jupiter.api.Test;
import smithy.java.codegen.server.test.model.*;
import smithy.java.codegen.server.test.service.EchoOperation;
import smithy.java.codegen.server.test.service.GetBeerOperation;
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


    @Test
    void testServer() throws InterruptedException {
        var server = Server.builder(URI.create("http://localhost:8080"))
            .addService(
                TestService.builder()
                    .addEchoOperation(new EchoOperationImpl())
                    .addGetBeerOperation(new GetBeer())
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
