/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.restjson;

import java.net.http.HttpHeaders;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import software.amazon.smithy.java.runtime.core.schema.*;
import software.amazon.smithy.java.runtime.core.serde.Codec;
import software.amazon.smithy.java.runtime.core.serde.DataStream;
import software.amazon.smithy.java.runtime.core.serde.EventStreamFrameEncodingProcessor;
import software.amazon.smithy.java.runtime.http.api.SmithyHttpRequest;
import software.amazon.smithy.java.runtime.http.binding.AwsFlowFrame;
import software.amazon.smithy.java.runtime.http.binding.AwsFlowFrameEncoder;
import software.amazon.smithy.java.runtime.http.binding.AwsFlowShapeDecoder;
import software.amazon.smithy.java.runtime.http.binding.AwsFlowShapeEncoder;
import software.amazon.smithy.java.runtime.http.binding.BindingMatcher;
import software.amazon.smithy.java.runtime.http.binding.HttpBinding;
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
import software.amazon.smithy.java.server.exceptions.InternalServerException;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpTrait;

public final class RestJsonProtocol extends ServerProtocol {


    private final Codec codec;

    public RestJsonProtocol(final Service service) {
        super(service);
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
        for (Operation<?, ?> operation : getOperations()) {
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
        var deser = HttpBinding.requestDeserializer()
            .inputShapeBuilder(shapeBuilder)
            .request(
                SmithyHttpRequest.builder()
                    .headers(job.request().getContext().get(HttpAttributes.HTTP_HEADERS))
                    .uri(job.request().getContext().get(HttpAttributes.HTTP_URI))
                    .method(job.request().getContext().get(HttpAttributes.HTTP_METHOD))
                    .body(getDataStream(job.request().getValue()))
                    .build()
            )
            .payloadCodec(codec);

        if (job.operation().getApiOperation() instanceof InputEventStreamingSdkOperation<?, ?, ?> streamingOp) {
            deser.eventDecoder(
                new AwsFlowShapeDecoder<>(
                    streamingOp::inputEventBuilder,
                    streamingOp.inputEventSchema(),
                    codec
                )
            );
        }

        try {
            deser.deserialize().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e.getCause());
        }

        job.request().setValue(new ShapeValue<>(shapeBuilder.build()));
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
        ApiOperation<?, ?> apiOperation = job.operation().getApiOperation();
        ShapeValue<? extends SerializableStruct> shapeValue = job.reply().getValue();
        Schema schema;
        SerializableStruct value = shapeValue.get();
        HttpErrorTrait errorTrait = null;
        if (value instanceof Exception e) {
            if (e instanceof ModeledApiException me && (schema = apiOperation.exceptionSchema(me)) != null) {
                errorTrait = schema.expectTrait(HttpErrorTrait.class);
            } else {
                schema = InternalServerException.SCHEMA;
                value = new InternalServerException(e);
                errorTrait = schema.expectTrait(HttpErrorTrait.class);
            }
        } else {
            schema = apiOperation.outputSchema();
        }
        HttpBindingSerializer serializer = new HttpBindingSerializer(
            schema.getTrait(HttpTrait.class),
            codec,
            BindingMatcher.responseMatcher(),
            null
        );
        serializer.writeStruct(schema, value);
        serializer.flush();
        int responseStatus = errorTrait != null ? errorTrait.getCode() : 200;
        HttpHeaders headers = serializer.getHeaders();
        if (errorTrait != null) {
            headers = addHeader(headers, "X-Amzn-ErrorType", schema.id().getName());
        }
        job.reply().context().put(HttpAttributes.HTTP_HEADERS, headers);
        job.reply().context().put(HttpAttributes.STATUS_CODE, responseStatus);

        if (apiOperation instanceof OutputEventStreamingSdkOperation<?, ?, ?> outputStreamingOp) {
            EventStreamFrameEncodingProcessor<AwsFlowFrame, ?> stream = new EventStreamFrameEncodingProcessor<>(
                serializer.getEventStream(),
                new AwsFlowShapeEncoder<>(
                    outputStreamingOp.outputEventSchema(),
                    codec
                ),
                new AwsFlowFrameEncoder()
            );
            job.reply().setValue(new ReactiveByteValue(stream));
        } else if (apiOperation.streamingOutput()) {
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

    private HttpHeaders addHeader(HttpHeaders headers, String name, String value) {
        Map<String, List<String>> copy = new HashMap<>(headers.map());
        copy.put(name, List.of(value));
        return HttpHeaders.of(copy, (k, v) -> true);
    }
}
