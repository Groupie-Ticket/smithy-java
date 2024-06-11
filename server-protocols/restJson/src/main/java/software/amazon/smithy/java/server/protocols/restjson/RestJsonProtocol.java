/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.restjson;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import software.amazon.smithy.java.runtime.core.schema.ApiOperation;
import software.amazon.smithy.java.runtime.core.schema.InputEventStreamingSdkOperation;
import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;
import software.amazon.smithy.java.runtime.core.schema.ShapeBuilder;
import software.amazon.smithy.java.runtime.core.serde.Codec;
import software.amazon.smithy.java.runtime.core.serde.DataStream;
import software.amazon.smithy.java.runtime.http.binding.AwsFlowShapeDecoder;
import software.amazon.smithy.java.runtime.http.binding.BindingMatcher;
import software.amazon.smithy.java.runtime.http.binding.HttpBindingDeserializer;
import software.amazon.smithy.java.runtime.http.binding.HttpBindingSerializer;
import software.amazon.smithy.java.runtime.json.JsonCodec;
import software.amazon.smithy.java.server.Operation;
import software.amazon.smithy.java.server.Service;
import software.amazon.smithy.java.server.core.ByteValue;
import software.amazon.smithy.java.server.core.Job;
import software.amazon.smithy.java.server.core.ReactiveByteValue;
import software.amazon.smithy.java.server.core.ResolutionRequest;
import software.amazon.smithy.java.server.core.ServerProtocol;
import software.amazon.smithy.java.server.core.ShapeValue;
import software.amazon.smithy.java.server.core.Value;
import software.amazon.smithy.java.server.core.attributes.HttpAttributes;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpTrait;

final class RestJsonProtocol extends ServerProtocol {

    private final List<Operation<?, ?>> operations = new ArrayList<>();
    private final Codec codec;

    public RestJsonProtocol(final Service service) {
        this.operations.addAll(service.getAllOperations());
        this.codec = JsonCodec.builder().useJsonName(true).useTimestampFormat(true).build();
    }

    @Override
    public ShapeId getProtocolId() {
        return ShapeId.from("aws.protocols#restJson1");
    }

    @Override
    public Operation<?, ?> resolveOperation(ResolutionRequest request) {
        String path = request.getUri().getPath();
        UriPattern uri = UriPattern.parse(path);
        Operation<?, ?> selectedOperation = null;
        for (Operation<?, ?> operation : operations) {
            UriPattern uriPattern = operation.getApiOperation().schema().expectTrait(HttpTrait.class).getUri();
            if (uriPattern.equals(uri)) {
                selectedOperation = operation;
                break;
            }
        }
        return selectedOperation;
    }

    @Override
    public void deserializeInput(Job job) {
        ShapeBuilder<? extends SerializableStruct> shapeBuilder = job.operation().getApiOperation().inputBuilder();
        HttpBindingDeserializer.Builder builder = HttpBindingDeserializer.builder()
            .request(true)
            .body(getDataStream(job.request().getValue()))
            .payloadCodec(codec)
            .shapeBuilder(shapeBuilder)
            .requestPath(job.request().getContext().get(HttpAttributes.HTTP_URI).getPath())
            .headers(job.request().getContext().get(HttpAttributes.HTTP_HEADERS));

        if (job.operation().getApiOperation() instanceof InputEventStreamingSdkOperation<?, ?, ?> streamingOp) {
            builder.eventDecoder(
                new AwsFlowShapeDecoder<>(
                    streamingOp::inputEventBuilder,
                    streamingOp.inputEventSchema(),
                    codec
                )
            );
        }

        job.request().setValue(new ShapeValue<>(shapeBuilder.deserialize(builder.build()).build()));

    }

    private DataStream getDataStream(Value value) {
        if (value instanceof ByteValue bv) {
            return DataStream.ofBytes(bv.get());
        } else if (value instanceof ReactiveByteValue rbv) {
            return DataStream.ofPublisher(rbv.get(), null, -1);
        } else {
            throw new IllegalStateException("Unexpected type: " + value.getClass());
        }
    }

    @Override
    public void serializeOutput(Job job) {
        ApiOperation<?, ?> sdkOperation = job.operation().getApiOperation();
        ShapeValue<? extends SerializableStruct> shapeValue = job.reply().getValue();
        HttpBindingSerializer serializer = new HttpBindingSerializer(
            null,
            codec,
            BindingMatcher.responseMatcher(),
            null
        );
        serializer.writeStruct(sdkOperation.outputSchema(), shapeValue.get());
        serializer.flush();
        job.reply().context().put(HttpAttributes.HTTP_HEADERS, serializer.getHeaders());

        if (sdkOperation.streamingOutput()) {
            job.reply().setValue(new ReactiveByteValue(serializer.getBody()));
        } else {
            DataStream dataStream = serializer.getBody();
            try {
                job.reply().setValue(new ByteValue(dataStream.asBytes().toCompletableFuture().get()));
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
