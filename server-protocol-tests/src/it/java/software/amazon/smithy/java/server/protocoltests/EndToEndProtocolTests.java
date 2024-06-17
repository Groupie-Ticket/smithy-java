/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocoltests;

import aws.protocoltests.restjson.model.PayloadConfig;
import aws.protocoltests.restjson.model.TestPayloadStructureInput;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import org.junit.jupiter.api.AfterAll;
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
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.protocoltests.traits.AppliesTo;
import software.amazon.smithy.protocoltests.traits.HttpRequestTestCase;
import software.amazon.smithy.protocoltests.traits.HttpRequestTestsTrait;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EndToEndProtocolTests {
    // If this is non-empty, only the test names (from the trait, not the operation name) within will run
    private static final Set<String> ONLY_RUN_THESE_TESTS = Set.of();

    private static final Set<ShapeId> REMOVED_OPERATIONS = Set.of(
        ShapeId.from("aws.protocoltests.restjson#RecursiveShapes")
    );

    private static final Map<String, SerializableShape> MANUAL_EXPECTATIONS = Map.of(
        // Weird one: httpPayload binging moves this test's "{}" to apply to payloadConfig, but the
        // document-based assertion is just an empty structure
        "RestJsonHttpWithEmptyStructurePayload",
        TestPayloadStructureInput.builder().payloadConfig(PayloadConfig.builder().build()).build()
    );

    private record TestOperation(
        ShapeId operationId, MockOperation mock, Supplier<ShapeBuilder<?>> builderSupplier,
        List<HttpRequestTestCase> requestTestCases
    ) {}

    private record TestService(
        ShapeId serviceId,
        List<TestOperation> operations,
        Server endpoint,
        int testPort,
        Service service
    ) {}

    private static final List<TestService> testServices = new ArrayList<>();

    private static final Map<URI, TestClient> clients = new ConcurrentHashMap<>();

    public static final String NS = "aws.protocoltests.restjson";

    @BeforeAll
    public static void beforeAll() throws Throwable {
        AtomicInteger testPort = new AtomicInteger(8020);
        MethodHandles.Lookup caller = MethodHandles.lookup();

        Model base = Model.assembler(EndToEndProtocolTests.class.getClassLoader())
            .discoverModels(EndToEndProtocolTests.class.getClassLoader())
            .assemble()
            .unwrap();

        ModelTransformer transformer = ModelTransformer.create();
        Model filtered = filterModel(transformer, base);
        for (ServiceShape serviceShape : filtered.getServiceShapes()) {
            Model serviceModel = performDefaultTransformations(serviceShape, transformer, filtered);
            SymbolProvider sp = SymbolProvider.cache(new ServiceJavaSymbolProvider(serviceModel, serviceShape, NS));

            try {
                Class<?> serviceClass = Class.forName(sp.toSymbol(serviceShape).toString());
                AtomicReference<Object> builderRef = new AtomicReference<>(
                    serviceClass.getDeclaredMethod("builder").invoke(null)
                );

                List<TestOperation> testOperations = new ArrayList<>();

                serviceShape.getAllOperations()
                    .stream()
                    .map(serviceModel::getShape)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(s -> (OperationShape) s)
                    .forEach(operationShape -> {
                        var mock = new MockOperation(operationShape.getId());
                        Supplier<ShapeBuilder<?>> inputBuilderSupplier;
                        try {
                            Class<?> inputType = Class.forName(
                                sp.toSymbol(serviceModel.expectShape(operationShape.getInputShape()))
                                    .toString()
                            );
                            Class<?> inputBuilderType = Class.forName(
                                sp.toSymbol(serviceModel.expectShape(operationShape.getInputShape()))
                                    .toString() + "$Builder"
                            );
                            MethodHandle builder = caller.findStatic(
                                inputType,
                                "builder",
                                MethodType.methodType(inputBuilderType)
                            );
                            inputBuilderSupplier = MethodHandleProxies.asInterfaceInstance(Supplier.class, builder);
                        } catch (Throwable t) {
                            throw new RuntimeException(t);
                        }
                        testOperations.add(
                            new TestOperation(
                                operationShape.getId(),
                                mock,
                                inputBuilderSupplier,
                                operationShape.getTrait(HttpRequestTestsTrait.class)
                                    .map(
                                        hrt -> hrt.getTestCases()
                                            .stream()
                                            .filter(
                                                tc -> tc.getAppliesTo().orElse(AppliesTo.SERVER) == AppliesTo.SERVER
                                            )
                                            .toList()
                                    )
                                    .orElse(Collections.emptyList())
                            )
                        );
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
                                        ).bindTo(mock)
                                    )
                                )
                            );

                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                    });
                Class<?> buildStage = Class.forName(serviceClass.getName() + "$BuildStage");
                MethodHandle build = caller.findVirtual(buildStage, "build", MethodType.methodType(serviceClass));


                Service service = (Service) build.invoke(builderRef.get());
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
            } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException
                | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @AfterAll
    public static void tearDown() {
        for (TestService testService : testServices) {
            testService.endpoint().stop();
        }
        for (TestClient c : clients.values()) {
            c.shutdown();
        }
    }

    private static Model filterModel(ModelTransformer transformer, Model base) {
        Model filtered = transformer.removeShapesIf(
            base,
            s -> s instanceof ServiceShape
                && !s.getId().getNamespace().equals(NS)
        );
        filtered = transformer.removeShapesIf(filtered, s -> REMOVED_OPERATIONS.contains(s.getId()));
        return transformer.removeUnreferencedShapes(filtered);
    }

    private static Model performDefaultTransformations(
        ServiceShape serviceShape,
        ModelTransformer transformer,
        Model filtered
    ) {
        filtered = transformer.copyServiceErrorsToOperations(filtered, serviceShape);
        filtered = transformer.flattenAndRemoveMixins(filtered);
        return transformer.createDedicatedInputAndOutput(filtered, "Input", "Output");
    }

    @TestTemplate
    @ExtendWith(RequestTestInvocationContextProvider.class)
    void requestTest(
        URI endpoint,
        HttpRequest rawRequest,
        SerializableShape deserializedRequest,
        MockOperation mock
    ) {
        var captor = mock.expectRequest();
        var c = getClient(endpoint);
        c.sendRequest(rawRequest);
        assertEquals(deserializedRequest, captor.get());
    }

    private TestClient getClient(URI endpoint) {
        return clients.computeIfAbsent(endpoint, $ -> new TestClient(endpoint));
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
                                    to.builderSupplier(),
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

    record HttpRequest(
        String method, String uri, List<String> queryParams, Map<String, String> headers,
        Optional<String> body, Optional<String> bodyMediaType
    ) {}

}
