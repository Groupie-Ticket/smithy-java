/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.restjson;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import software.amazon.smithy.java.runtime.core.schema.ApiOperation;
import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;
import software.amazon.smithy.java.runtime.core.schema.ShapeBuilder;
import software.amazon.smithy.java.runtime.core.serde.Codec;
import software.amazon.smithy.java.runtime.core.serde.DataStream;
import software.amazon.smithy.java.runtime.http.binding.BindingMatcher;
import software.amazon.smithy.java.runtime.http.binding.HttpBindingDeserializer;
import software.amazon.smithy.java.runtime.http.binding.HttpBindingSerializer;
import software.amazon.smithy.java.runtime.json.JsonCodec;
import software.amazon.smithy.java.server.Operation;
import software.amazon.smithy.java.server.Service;
import software.amazon.smithy.java.server.core.ByteValue;
import software.amazon.smithy.java.server.core.Job;
import software.amazon.smithy.java.server.core.ServerProtocolHandler;
import software.amazon.smithy.java.server.core.ShapeValue;
import software.amazon.smithy.java.server.core.attributes.HttpAttributes;
import software.amazon.smithy.java.server.core.attributes.ServiceAttributes;
import software.amazon.smithy.java.server.exceptions.UnknownOperationException;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpTrait;

final class RestJsonProtocolHandler extends ServerProtocolHandler {

    private final List<Operation<?, ?>> operations = new ArrayList<>();
    private final Codec codec;

    public RestJsonProtocolHandler(final Service service) {
        this.operations.addAll(service.getAllOperations());
        this.codec = JsonCodec.builder().useJsonName(true).useTimestampFormat(true).build();
    }

    @Override
    public ShapeId getProtocolId() {
        return ShapeId.from("aws.protocols#restJson1");
    }

    @Override
    public void doBefore(Job job) {
        if (!claim(job)) {
            return;
        }

        String path = job.getRequest().getContext().get(HttpAttributes.HTTP_URI).getPath();
        var headers = job.getRequest().getContext().get(HttpAttributes.HTTP_HEADERS);
        UriPattern uri = UriPattern.parse(path);
        Operation<?, ?> selectedOperation = null;
        for (Operation<?, ?> operation : operations) {
            UriPattern uriPattern = operation.getApiOperation().schema().expectTrait(HttpTrait.class).getUri();
            if (uriPattern.equals(uri)) {
                selectedOperation = operation;
                break;
            }
        }

        if (selectedOperation == null) {
            throw new UnknownOperationException("Unknown operation: " + uri);
        }

        job.getRequest().getContext().put(ServiceAttributes.OPERATION, selectedOperation);
        ByteValue requestBody = job.getRequest().getValue();
        ShapeBuilder<? extends SerializableStruct> shapeBuilder = selectedOperation.getApiOperation().inputBuilder();
        HttpBindingDeserializer deserializer = HttpBindingDeserializer.builder()
            .request(true)
            .body(DataStream.ofBytes(requestBody.get()))
            .payloadCodec(codec)
            .shapeBuilder(shapeBuilder)
            .requestPath(path)
            .headers(headers)
            .build();
        job.getRequest().setValue(new ShapeValue<>(shapeBuilder.deserialize(deserializer).build()));

    }

    @Override
    public void doAfter(Job job) {
        if (!isClaimedByThis(job) || job.getFailure().isPresent()) {
            return;
        }
        ApiOperation<?, ?> sdkOperation = job.getRequest()
            .getContext()
            .get(ServiceAttributes.OPERATION)
            .getApiOperation();
        ShapeValue<? extends SerializableStruct> shapeValue = job.getReply().getValue();
        HttpBindingSerializer serializer = new HttpBindingSerializer(
            null,
            codec,
            BindingMatcher.responseMatcher(),
            null
        );
        serializer.writeStruct(sdkOperation.outputSchema(), shapeValue.get());
        serializer.flush();
        DataStream dataStream = serializer.getBody();
        try {
            job.getReply().setValue(new ByteValue(dataStream.asBytes().toCompletableFuture().get()));
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
