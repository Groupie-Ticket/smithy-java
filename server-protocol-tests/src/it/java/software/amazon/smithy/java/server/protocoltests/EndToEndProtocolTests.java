/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocoltests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import aws.protocoltests.restjson.model.OmitsSerializingEmptyListsInput;
import aws.protocoltests.restjson.model.PayloadConfig;
import aws.protocoltests.restjson.model.TestPayloadBlobInput;
import aws.protocoltests.restjson.model.TestPayloadStructureInput;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.java.codegen.CodegenUtils;
import software.amazon.smithy.java.codegen.server.ServerSymbolProperties;
import software.amazon.smithy.java.codegen.server.ServiceJavaSymbolProvider;
import software.amazon.smithy.java.runtime.core.schema.SerializableShape;
import software.amazon.smithy.java.runtime.core.schema.ShapeBuilder;
import software.amazon.smithy.java.server.RequestContext;
import software.amazon.smithy.java.server.Server;
import software.amazon.smithy.java.server.Service;
import software.amazon.smithy.java.server.netty.NettyServerBuilder;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.protocoltests.traits.AppliesTo;
import software.amazon.smithy.protocoltests.traits.HttpMalformedRequestTestCase;
import software.amazon.smithy.protocoltests.traits.HttpMalformedRequestTestsTrait;
import software.amazon.smithy.protocoltests.traits.HttpMalformedResponseBodyDefinition;
import software.amazon.smithy.protocoltests.traits.HttpRequestTestCase;
import software.amazon.smithy.protocoltests.traits.HttpRequestTestsTrait;
import software.amazon.smithy.protocoltests.traits.HttpResponseTestCase;
import software.amazon.smithy.protocoltests.traits.HttpResponseTestsTrait;

public class EndToEndProtocolTests {
    // If this is non-empty, only the test names (from the trait, not the operation name) within will run
    private static final Set<String> ONLY_RUN_THESE_TESTS = Set.of();

    // These tests we don't code generate because they cannot be code generated
    private static final Set<ShapeId> REMOVED_OPERATIONS = Set.of(
        ShapeId.from("aws.protocoltests.restjson#RecursiveShapes")
    );

    // These tests we have purposefully not implemented yet
    private static final Set<String> SKIPPED_TESTS = Set.of(
        // we don't support content-encoding yet
        "SDKAppliedContentEncoding_restJson1",
        "SDKAppendedGzipAfterProvidedEncoding_restJson1"
    );

    private static final Map<String, SerializableShape> MANUAL_EXPECTATIONS = Map.of(
        // Weird one: httpPayload binging moves this test's "{}" to apply to payloadConfig, but the
        // document-based assertion is just an empty structure
        "RestJsonHttpWithEmptyStructurePayload",
        TestPayloadStructureInput.builder().payloadConfig(PayloadConfig.builder().build()).build(),
        "RestJsonHttpWithHeadersButNoPayload",
        TestPayloadStructureInput.builder().testId("t-12345").payloadConfig(PayloadConfig.builder().build()).build(),

        // Query parameter protocol tests generally leave off values for unspecified query string parameters
        // but not this one, which insists that they be deserialized as empty lists
        "RestJsonOmitsEmptyListQueryValues",
        OmitsSerializingEmptyListsInput.builder().build(),

        // will be fixed upstream: https://github.com/smithy-lang/smithy/pull/2336
        "RestJsonHttpWithEmptyBlobPayload",
        TestPayloadBlobInput.builder().contentType("application/octet-stream").build()
    );

    private static final List<TestService> testServices = new ArrayList<>();

    public static final String NS = "aws.protocoltests.restjson";

    @BeforeAll
    public static void beforeAll() throws Throwable {
        AtomicInteger testPort = new AtomicInteger(8020);
        MethodHandles.Lookup caller = MethodHandles.lookup();

        for (Map.Entry<ProtocolTestDiscovery.ProtocolTestService, List<OperationShape>> entry : ProtocolTestDiscovery
            .get()
            .discoverTests(EndToEndProtocolTests::testFilter)
            .entrySet()) {

            var serviceModel = entry.getKey().serviceModel();
            var serviceShape = entry.getKey().service();
            var sp = SymbolProvider.cache(new ServiceJavaSymbolProvider(serviceModel, serviceShape, NS));

            var testOperations = getTestOperations(entry.getValue(), sp, serviceModel, caller);

            Service service = buildService(testOperations, sp, serviceModel, serviceShape, caller);
            int port = testPort.incrementAndGet();
            Server endpoint = new NettyServerBuilder(URI.create("http://localhost:" + port))
                .addService(service)
                .build();
            endpoint.start();

            testServices.add(
                new TestService(
                    serviceShape.getId(),
                    testOperations,
                    endpoint,
                    port,
                    service
                )
            );
        }
    }

    @AfterAll
    public static void tearDown() {
        for (TestService testService : testServices) {
            testService.endpoint().stop();
        }
    }

    @TestTemplate
    @ExtendWith(RequestTestInvocationContextProvider.class)
    void requestTest(
        String testId,
        URI endpoint,
        HttpRequest rawRequest,
        SerializableShape deserializedRequest,
        MockOperation mock
    ) {
        Assumptions.assumeFalse(
            SKIPPED_TESTS.contains(testId),
            testId + " is currently unsupported"
        );
        var captor = mock.expectRequest();
        TestClient.get(endpoint).sendRequest(rawRequest);
        var expected = Normalizer.normalize(deserializedRequest);
        var actual = captor.get();
        assertEquals(expected, actual);
    }

    @TestTemplate
    @ExtendWith(ResponseTestInvocationContextProvider.class)
    void responseTest(
        String testId,
        URI endpoint,
        ServiceCoordinate serviceCoordinate,
        HttpResponse expectedResponse,
        SerializableShape deserializedResponse,
        MockOperation mock
    ) {
        Assumptions.assumeFalse(
            SKIPPED_TESTS.contains(testId),
            testId + " is currently unsupported"
        );
        mock.setResponse(deserializedResponse);
        var serviceResponse = TestClient.get(endpoint)
            .call(
                getTestRequest(endpoint, serviceCoordinate.serviceId(), serviceCoordinate.operationId())
            );

        assertEquals(expectedResponse.code, serviceResponse.status().code());

        assertHeaders(expectedResponse, serviceResponse);

        expectedResponse.body.ifPresent(expectedBody -> {
            expectedResponse.bodyMediaType.ifPresentOrElse(expectedMediaType -> {
                if (expectedMediaType.equals("application/json")) {
                    assertEquals(
                        Normalizer.normalize(ObjectNode.parse(expectedBody)),
                        Normalizer.normalize(
                            ObjectNode.parse(serviceResponse.content().toString(StandardCharsets.UTF_8))
                        )
                    );
                } else {
                    assertEquals(expectedBody, serviceResponse.content().toString(StandardCharsets.UTF_8));
                }
            }, () -> {
                if (expectedBody.isEmpty()) {
                    // maybe a cop-out, but we generally code generate with the transformation
                    // that creates an independent input and output no matter what.
                    // So the server cannot help but send back a {} when the model is Unit
                    // it never ees the unit
                    assertThat(serviceResponse.content().toString(StandardCharsets.UTF_8))
                        .isIn("", "{}");
                } else {
                    assertEquals(expectedBody, serviceResponse.content().toString(StandardCharsets.UTF_8));
                }
            });
        });
    }

    @TestTemplate
    @ExtendWith(MalformedRequestTestInvocationContextProvider.class)
    void malformedRequestTest(
        String testId,
        URI endpoint,
        HttpRequest request,
        HttpResponse expectedResponse,
        HttpMalformedResponseBodyDefinition bodyDefinition,
        MockOperation mock
    ) {
        Assumptions.assumeFalse(
            SKIPPED_TESTS.contains(testId),
            testId + " is currently unsupported"
        );
        var captor = mock.rejectRequest();
        var response = TestClient.get(endpoint).sendRequest(request);

        assertNull(captor.get());

        assertEquals(expectedResponse.code(), response.status().code());
        assertHeaders(expectedResponse, response);

        if (bodyDefinition == null) {
            return;
        }

        String responseContent = response.content().toString(StandardCharsets.UTF_8);
        bodyDefinition.getContents().ifPresent(contents -> {
            if ("application/json".equals(bodyDefinition.getMediaType())) {
                assertEquals(
                    Normalizer.normalize(ObjectNode.parse(contents)),
                    Normalizer.normalize(
                        ObjectNode.parse(responseContent)
                    )
                );
            } else {
                assertEquals(contents, responseContent);
            }
        });

        bodyDefinition.getMessageRegex().ifPresent(regex -> {
            assertTrue(
                responseContent.matches(regex),
                "Expected message mtching " + regex + ", was " + responseContent
            );
        });
    }

    private static void assertHeaders(HttpResponse expectedResponse, FullHttpResponse serviceResponse) {
        for (var headerEntry : expectedResponse.headers.entrySet()) {
            if (headerEntry.getKey().toLowerCase(Locale.US).startsWith("x-")) {
                assertEquals(
                    headerEntry.getValue(),
                    convertResponseHeader(
                        headerEntry.getKey(),
                        serviceResponse.headers().getAll(headerEntry.getKey())
                    ),
                    "Mismatch for expected header: " + headerEntry.getKey()
                );
            } else {
                assertEquals(
                    headerEntry.getValue(),
                    serviceResponse.headers().get(headerEntry.getKey()),
                    "Mismatch for expected header " + headerEntry.getKey()
                );
            }
        }
    }

    private static String convertResponseHeader(String key, List<String> values) {
        if (!key.equalsIgnoreCase("x-stringlist")) {
            return String.join(", ", values);
        }
        return values.stream().map(value -> {
            if (value.chars()
                .anyMatch(c -> TestClient.isHeaderDelimiter((char) c) || Character.isWhitespace((char) c))) {
                return '"' + value.replaceAll("[\\s\"]", "\\\\$0") + '"';
            }
            return value;
        }).collect(Collectors.joining(", "));
    }

    private DefaultFullHttpRequest getTestRequest(URI endpoint, ShapeId serviceId, ShapeId operationId) {
        return new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.POST,
            endpoint.getPath(),
            Unpooled.EMPTY_BUFFER,
            new DefaultHttpHeaders().add("x-protocol-test-service", serviceId.toString())
                .add("x-protocol-test-operation", operationId.toString()),
            new DefaultHttpHeaders()
        );
    }

    private static boolean testFilter(Shape s) {
        if (s instanceof ServiceShape serv) {
            return !serv.getId().getNamespace().equals(NS);
        }

        if (s instanceof OperationShape op) {
            return REMOVED_OPERATIONS.contains(op.getId());
        }

        return false;
    }

    private static Service buildService(
        List<TestOperation> testOperations,
        SymbolProvider sp,
        Model serviceModel,
        ServiceShape serviceShape,
        MethodHandles.Lookup caller
    ) throws Throwable {
        Class<?> serviceClass = Class.forName(sp.toSymbol(serviceShape).toString());
        AtomicReference<Object> builderRef = new AtomicReference<>(
            serviceClass.getDeclaredMethod("builder").invoke(null)
        );

        for (TestOperation testOperation : testOperations) {
            var operationShape = serviceModel.expectShape(testOperation.operationId());
            String cappedName = CodegenUtils.getDefaultName(operationShape, serviceShape);
            try {
                Class<?> operationType = Class.forName(
                    sp.toSymbol(operationShape)
                        .expectProperty(ServerSymbolProperties.STUB_OPERATION)
                        .toString()
                );
                Class<?> stage = Class.forName(serviceClass.getName() + "$" + cappedName + "Stage");
                Method addMethod = stage.getDeclaredMethod(
                    "add" + cappedName + "Operation",
                    operationType
                );

                builderRef.set(
                    addMethod.invoke(
                        builderRef.get(),
                        MethodHandleProxies.asInterfaceInstance(
                            operationType,
                            caller.findVirtual(
                                MockOperation.class,
                                "apply",
                                MethodType.methodType(Object.class, Object.class, RequestContext.class)
                            ).bindTo(testOperation.mock())
                        )
                    )
                );

            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        Class<?> buildStage = Class.forName(serviceClass.getName() + "$BuildStage");
        MethodHandle build = caller.findVirtual(buildStage, "build", MethodType.methodType(serviceClass));

        return (Service) build.invoke(builderRef.get());
    }

    private static List<TestOperation> getTestOperations(
        List<OperationShape> operations,
        SymbolProvider sp,
        Model serviceModel,
        MethodHandles.Lookup caller
    ) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
        List<TestOperation> testOperations = new ArrayList<>();
        for (OperationShape operationShape : operations) {
            var mock = new MockOperation(operationShape.getId());

            Supplier<ShapeBuilder<?>> inputBuilderSupplier = getShapeBuilderSupplier(
                operationShape.getInputShape(),
                sp,
                serviceModel,
                caller
            );

            Supplier<ShapeBuilder<?>> outputBuilderSupplier = getShapeBuilderSupplier(
                operationShape.getOutputShape(),
                sp,
                serviceModel,
                caller
            );

            testOperations.add(
                new TestOperation(
                    operationShape.getId(),
                    mock,
                    inputBuilderSupplier,
                    operationShape.getTrait(HttpRequestTestsTrait.class)
                        .map(hrt -> hrt.getTestCasesFor(AppliesTo.SERVER))
                        .orElse(Collections.emptyList()),
                    outputBuilderSupplier,
                    operationShape.getTrait(HttpResponseTestsTrait.class)
                        .map(hrt -> hrt.getTestCasesFor(AppliesTo.SERVER))
                        .orElse(Collections.emptyList()),
                    operationShape.getTrait(HttpMalformedRequestTestsTrait.class)
                        .map(HttpMalformedRequestTestsTrait::getTestCases)
                        .orElse(Collections.emptyList())
                )
            );
        }
        return testOperations;
    }

    private static Supplier<ShapeBuilder<?>> getShapeBuilderSupplier(
        ShapeId buildableShapeId,
        SymbolProvider sp,
        Model serviceModel,
        MethodHandles.Lookup caller
    ) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
        var buildableShapeSymbol = sp.toSymbol(serviceModel.expectShape(buildableShapeId));

        Class<?> shapeType = Class.forName(
            buildableShapeSymbol.toString()
        );
        Class<?> shapeBuilderType = Class.forName(
            buildableShapeSymbol + "$Builder"
        );
        MethodHandle builder = caller.findStatic(
            shapeType,
            "builder",
            MethodType.methodType(shapeBuilderType)
        );
        return MethodHandleProxies.asInterfaceInstance(
            Supplier.class,
            builder
        );
    }

    public static class RequestTestInvocationContextProvider implements TestTemplateInvocationContextProvider {

        public RequestTestInvocationContextProvider() {}

        @Override
        public boolean supportsTestTemplate(ExtensionContext extensionContext) {
            return true;
        }

        @Override
        public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
            ExtensionContext extensionContext
        ) {
            List<TestTemplateInvocationContext> contexts = new ArrayList<>();
            for (TestService ts : testServices) {
                URI endpoint = URI.create("http://localhost:" + ts.testPort);
                for (TestOperation to : ts.operations()) {
                    for (HttpRequestTestCase tc : to.requestTestCases()) {
                        if (ONLY_RUN_THESE_TESTS.isEmpty() || ONLY_RUN_THESE_TESTS.contains(tc.getId())) {
                            contexts.add(
                                new RequestTestInvocationContext(
                                    endpoint,
                                    tc,
                                    to.mock(),
                                    to.inputBuilderSupplier(),
                                    MANUAL_EXPECTATIONS.get(tc.getId())
                                )
                            );
                        }
                    }
                }
            }
            return contexts.stream();
        }
    }

    public static class ResponseTestInvocationContextProvider implements TestTemplateInvocationContextProvider {

        public ResponseTestInvocationContextProvider() {}

        @Override
        public boolean supportsTestTemplate(ExtensionContext extensionContext) {
            return true;
        }

        @Override
        public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
            ExtensionContext extensionContext
        ) {
            List<TestTemplateInvocationContext> contexts = new ArrayList<>();
            for (TestService ts : testServices) {
                URI endpoint = URI.create("http://localhost:" + ts.testPort);
                for (TestOperation to : ts.operations()) {
                    for (HttpResponseTestCase tc : to.responseTestCases()) {
                        if (ONLY_RUN_THESE_TESTS.isEmpty() || ONLY_RUN_THESE_TESTS.contains(tc.getId())) {
                            contexts.add(
                                new ResponseTestInvocationContext(
                                    endpoint,
                                    ts.serviceId(),
                                    to.operationId(),
                                    tc,
                                    to.mock(),
                                    to.outputBuilderSupplier(),
                                    MANUAL_EXPECTATIONS.get(tc.getId())
                                )
                            );
                        }
                    }
                }
            }
            return contexts.stream();
        }
    }

    public static class MalformedRequestTestInvocationContextProvider implements TestTemplateInvocationContextProvider {

        public MalformedRequestTestInvocationContextProvider() {}

        @Override
        public boolean supportsTestTemplate(ExtensionContext extensionContext) {
            return true;
        }

        @Override
        public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
            ExtensionContext extensionContext
        ) {
            List<TestTemplateInvocationContext> contexts = new ArrayList<>();
            for (TestService ts : testServices) {
                URI endpoint = URI.create("http://localhost:" + ts.testPort);
                for (TestOperation to : ts.operations()) {
                    for (HttpMalformedRequestTestCase tc : to.malformedTestCases()) {
                        if (ONLY_RUN_THESE_TESTS.isEmpty() || ONLY_RUN_THESE_TESTS.contains(tc.getId())) {
                            contexts.add(
                                new MalformedRequestTestInvocationContext(
                                    endpoint,
                                    tc,
                                    to.mock()
                                )
                            );
                        }
                    }
                }
            }
            return contexts.stream();
        }
    }

    record ServiceCoordinate(ShapeId serviceId, ShapeId operationId) {}

    record HttpRequest(
        String method, String uri, List<String> queryParams, Map<String, String> headers,
        Optional<String> body, Optional<String> bodyMediaType
    ) {}

    record HttpResponse(
        int code, Map<String, String> headers, Optional<String> body, Optional<String> bodyMediaType
    ) {}

    private record TestOperation(
        ShapeId operationId, MockOperation mock,
        Supplier<ShapeBuilder<?>> inputBuilderSupplier,
        List<HttpRequestTestCase> requestTestCases,
        Supplier<ShapeBuilder<?>> outputBuilderSupplier,
        List<HttpResponseTestCase> responseTestCases,
        List<HttpMalformedRequestTestCase> malformedTestCases
    ) {}

    private record TestService(
        ShapeId serviceId,
        List<TestOperation> operations,
        Server endpoint,
        int testPort,
        Service service
    ) {}

}
