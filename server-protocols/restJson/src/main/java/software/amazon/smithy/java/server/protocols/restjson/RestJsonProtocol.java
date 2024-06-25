/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.restjson;

import static software.amazon.smithy.java.server.protocols.restjson.router.UriPattern.forSpecificityRouting;

import java.net.http.HttpHeaders;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import software.amazon.smithy.java.runtime.core.Context;
import software.amazon.smithy.java.runtime.core.schema.*;
import software.amazon.smithy.java.runtime.core.serde.Codec;
import software.amazon.smithy.java.runtime.core.serde.DataStream;
import software.amazon.smithy.java.runtime.core.serde.EventStreamFrameEncodingProcessor;
import software.amazon.smithy.java.runtime.core.serde.SerializationException;
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
import software.amazon.smithy.java.server.core.ResolutionResult;
import software.amazon.smithy.java.server.core.ServerProtocol;
import software.amazon.smithy.java.server.core.ShapeValue;
import software.amazon.smithy.java.server.core.Value;
import software.amazon.smithy.java.server.core.attributes.HttpAttributes;
import software.amazon.smithy.java.server.exceptions.InternalServerException;
import software.amazon.smithy.java.server.exceptions.SyntheticExceptions;
import software.amazon.smithy.java.server.protocols.restjson.router.UriMatcherMap;
import software.amazon.smithy.java.server.protocols.restjson.router.UriTreeMatcherMap;
import software.amazon.smithy.java.server.protocols.restjson.router.UrlEncoder;
import software.amazon.smithy.java.server.protocols.restjson.router.ValuedMatch;
import software.amazon.smithy.model.pattern.SmithyPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpTrait;

public final class RestJsonProtocol extends ServerProtocol {
    private static final Context.Key<ValuedMatch<Operation<?, ?>>> MATCH_KEY = Context.key("restjson-match");

    private final UriMatcherMap<Operation<?, ?>> matcher;
    private final Codec codec;

    public RestJsonProtocol(final Service service) {
        super(service);
        var builder = UriTreeMatcherMap.<Operation<?, ?>>builder();
        for (Operation<?, ?> operation : getOperations()) {
            builder.add(
                forSpecificityRouting(
                    operation.getApiOperation()
                        .schema()
                        .expectTrait(HttpTrait.class)
                        .getUri()
                        .toString()
                ),
                operation
            );
        }
        matcher = builder.build();
        this.codec = JsonCodec.builder()
            .useJsonName(true)
            .useTimestampFormat(true)
            .allowUnknownUnionMembers(false)
            .build();
    }

    @Override
    public ShapeId getProtocolId() {
        return ShapeId.from("aws.protocols#restJson1");
    }

    @Override
    public ResolutionResult resolveOperation(ResolutionRequest request) {
        var uri = request.uri().getPath();
        String rawQuery = request.uri().getRawQuery();
        if (rawQuery != null) {
            uri += "?" + rawQuery;
        }
        ValuedMatch<Operation<?, ?>> selectedOperation = matcher.match(uri);
        if (selectedOperation != null) {
            Context ctx = Context.create();
            ctx.put(MATCH_KEY, selectedOperation);
            return new ResolutionResult(selectedOperation.getValue(), this, ctx);
        }

        return null;
    }

    @Override
    public CompletableFuture<Void> deserializeInput(Job job) {
        ShapeBuilder<? extends SerializableStruct> shapeBuilder = job.operation().getApiOperation().inputBuilder();

        // todo - store the path match result
        ValuedMatch<Operation<?, ?>> selectedOperation = job.context().get(PROTOCOL_CONTEXT).get(MATCH_KEY);

        Map<String, String> labelValues = new HashMap<>();
        var encoder = new UrlEncoder();
        for (SmithyPattern.Segment label : job.operation()
            .getApiOperation()
            .schema()
            .expectTrait(HttpTrait.class)
            .getUri()
            .getLabels()) {
            var values = selectedOperation.getLabelValues(label.getContent());
            if (values != null) {
                labelValues.put(label.getContent(), encoder.decodeUriComponent(values.get(0)));
            }
        }

        HttpHeaders headers = job.request().getContext().get(HttpAttributes.HTTP_HEADERS);
        var deser = HttpBinding.requestDeserializer()
            .inputShapeBuilder(shapeBuilder)
            .pathLabelValues(labelValues)
            .request(
                SmithyHttpRequest.builder()
                    .headers(headers)
                    .uri(job.request().getContext().get(HttpAttributes.HTTP_URI))
                    .method(job.request().getContext().get(HttpAttributes.HTTP_METHOD))
                    .body(getDataStream(job.request().getValue(), headers))
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
            throw handleException(e);
        } catch (ExecutionException e) {
            throw handleException(e.getCause());
        } catch (RuntimeException e) {
            throw handleException(e);
        }

        job.request().setValue(new ShapeValue<>(shapeBuilder.build()));
        return CompletableFuture.completedFuture(null);
    }

    private static RuntimeException handleException(Throwable t) {
        if (t instanceof SerializationException se) {
            return new software.amazon.smithy.java.server.exceptions.SerializationException(se);
        }
        if (t instanceof ModeledApiException mse) {
            return mse;
        }
        return new InternalServerException(t);
    }

    private DataStream getDataStream(Value value, HttpHeaders headers) {
        if (value instanceof ByteValue bv) {
            byte[] bytes = bv.get();
            if (bytes == null || bytes.length == 0) {
                return DataStream.ofEmpty();
            }
            return DataStream.ofBytes(bv.get(), headers.firstValue("content-type").orElse(null));
        } else if (value instanceof ReactiveByteValue rbv) {
            return DataStream.ofPublisher(
                rbv.get(),
                headers.firstValue("content-type").orElse(null),
                headers.firstValue("content-length").map(Long::parseLong).orElse(-1L)
            );
        } else {
            throw new IllegalStateException("Unexpected type: " + value.getClass());
        }
    }

    @Override
    public CompletableFuture<Void> serializeOutput(Job job) {
        return job.getFailure()
            .map(t -> serializeError(job, t))
            .orElseGet(() -> {
                if (job.reply().getValue() instanceof ShapeValue<? extends SerializableStruct> shapeValue) {
                    return serializeReply(job, shapeValue.get());
                }
                return serializeReply(
                    job,
                    new InternalServerException(
                        new IllegalStateException(
                            "Unrecognized return type: "
                                + job.reply().getValue()
                        )
                    )
                );
            });

    }

    private CompletableFuture<Void> serializeReply(Job job, SerializableStruct value) {
        ApiOperation<?, ?> apiOperation = job.operation().getApiOperation();
        Schema schema;
        HttpErrorTrait errorTrait = null;
        if (value instanceof Exception e) {
            schema = getExceptionSchema(apiOperation, e);
            if (schema == null) {
                return serializeReply(job, new InternalServerException(e));
            }
            errorTrait = schema.expectTrait(HttpErrorTrait.class);
        } else {
            schema = apiOperation.outputSchema();
        }
        HttpBindingSerializer serializer = new HttpBindingSerializer(
            apiOperation.schema().expectTrait(HttpTrait.class),
            codec,
            BindingMatcher.responseMatcher(),
            null
        );
        serializer.writeStruct(schema, value);
        serializer.flush();
        int responseStatus = errorTrait != null ? errorTrait.getCode() : serializer.getResponseStatus();
        HttpHeaders headers = serializer.getHeaders();
        if (errorTrait != null) {
            headers = addHeader(headers, "X-Amzn-ErrorType", schema.id().getName());
        }
        job.reply().context().put(HttpAttributes.HTTP_HEADERS, headers);
        job.reply().context().put(HttpAttributes.STATUS_CODE, responseStatus);

        if (apiOperation instanceof OutputEventStreamingSdkOperation<?, ?, ?> outputStreamingOp && errorTrait == null) {
            EventStreamFrameEncodingProcessor<AwsFlowFrame, ?> stream = new EventStreamFrameEncodingProcessor<>(
                serializer.getEventStream(),
                new AwsFlowShapeEncoder<>(
                    outputStreamingOp.outputEventSchema(),
                    codec,
                    RestJsonProtocol::handlEventStreamingException
                ),
                new AwsFlowFrameEncoder()
            );
            job.reply().setValue(new ReactiveByteValue(stream));
        } else if (apiOperation.streamingOutput() && errorTrait == null) {
            job.reply().setValue(new ReactiveByteValue(serializer.getBody()));
        } else {
            DataStream dataStream = serializer.getBody();
            try {
                job.reply().setValue(new ByteValue(dataStream.asBytes().toCompletableFuture().get()));
                job.setFailure(null);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    private static EventStreamingException handlEventStreamingException(Throwable e) {
        if (e instanceof SerializationException se) {
            return new EventStreamingException("SerializationException", se.getMessage());
        } else if (e instanceof ModeledApiException mae) {
            return SyntheticExceptions.getSchema(mae.getShapeId())
                .map(s -> new EventStreamingException(s.id().getName(), mae.getMessage()))
                .orElseGet(() -> new EventStreamingException("InternalServerError", "internal server error"));
        }
        return new EventStreamingException("InternalServerError", "internal server error");
    }

    private static Schema getExceptionSchema(ApiOperation<?, ?> apiOperation, Exception e) {
        if (e instanceof ModeledApiException mae) {
            Schema s = apiOperation.exceptionSchema(mae);
            if (s == null) {
                return SyntheticExceptions.getSchema(mae.getShapeId()).orElse(null);
            }
            return s;
        }
        return null;
    }

    private CompletableFuture<Void> serializeError(Job job, Throwable t) {
        if (t instanceof ModeledApiException mae) {
            return serializeReply(job, mae);
        }
        return serializeReply(job, new InternalServerException(t));
    }

    private HttpHeaders addHeader(HttpHeaders headers, String name, String value) {
        Map<String, List<String>> copy = new HashMap<>(headers.map());
        copy.put(name, List.of(value));
        return HttpHeaders.of(copy, (k, v) -> true);
    }
}
