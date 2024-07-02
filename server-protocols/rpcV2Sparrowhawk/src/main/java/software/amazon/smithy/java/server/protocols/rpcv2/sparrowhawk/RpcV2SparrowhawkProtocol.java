/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.rpcv2.sparrowhawk;

import java.net.http.HttpHeaders;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import software.amazon.smithy.java.runtime.core.Context;
import software.amazon.smithy.java.runtime.core.schema.ApiOperation;
import software.amazon.smithy.java.runtime.core.schema.ModeledApiException;
import software.amazon.smithy.java.runtime.core.schema.Schema;
import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;
import software.amazon.smithy.java.runtime.core.serde.NoInitialEventException;
import software.amazon.smithy.java.runtime.core.serde.SerializationException;
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
import software.amazon.smithy.java.server.core.http.HttpMethod;
import software.amazon.smithy.java.server.exceptions.InternalServerException;
import software.amazon.smithy.java.server.exceptions.SyntheticExceptions;
import software.amazon.smithy.java.sparrowhawk.SparrowhawkDeserializer;
import software.amazon.smithy.java.sparrowhawk.codec.NonBufferingInitialEventDecoder;
import software.amazon.smithy.java.sparrowhawk.codec.SparrowhawkCodec;
import software.amazon.smithy.java.sparrowhawk.codec.SparrowhawkCodecFactory;
import software.amazon.smithy.java.sparrowhawk.codec.SparrowhawkCodecFactoryIndex;
import software.amazon.smithy.java.sparrowhawk.codec.SparrowhawkPayloadBufferer;
import software.amazon.smithy.java.sparrowhawk.codec.SparrowhawkSigV4Frame;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.utils.Pair;

final class RpcV2SparrowhawkProtocol extends ServerProtocol {
    private static final Context.Key<SparrowhawkCodec> SPARROWHAWK_CODEC = Context.key("sparrowhawkCodec");

    private final SparrowhawkCodecFactory sparrowhawkCodecFactory;
    private final boolean sigv4;

    RpcV2SparrowhawkProtocol(Service service) {
        super(service);
        var serviceId = service.getSchema().getId().toString();
        this.sparrowhawkCodecFactory = Objects.requireNonNull(
            SparrowhawkCodecFactoryIndex.getCodecFactory(
                serviceId
            ),
            "Couldn't find a CodecFactory for service " + serviceId
        );
        this.sigv4 = service.getSchema().getAwsAuthModes().contains(ShapeId.from("aws.auth#sigv4"));
    }

    @Override
    public ShapeId getProtocolId() {
        return ShapeId.from("smithy.protocols#rpcv2Sparrowhawk");
    }

    @Override
    public ResolutionResult resolveOperation(ResolutionRequest request) {
        if (!HttpMethod.POST.equals(request.method())) {
            return null;
        }
        List<String> smithyHeaders = request.headers().map().get("smithy-protocol");
        if (smithyHeaders == null || smithyHeaders.size() != 1 || !"rpc-v2-sparrowhawk".equals(smithyHeaders.get(0))) {
            return null;
        }
        String contentType = request.headers().map().get("Content-Type").get(0);
        if (!("application/vnd.amazon.sparrowhawk".equals(contentType) || "application/vnd.amazon.eventstream".equals(
            contentType
        ))) {
            return null;
        }
        Pair<String, String> serviceOperation = RpcV2PathParser.parseRpcV2Path(request.uri().getPath());
        return new ResolutionResult(
            getService().getOperation(serviceOperation.right),
            this,
            null
        );
    }

    @Override
    public CompletableFuture<Void> deserializeInput(Job job) {
        SparrowhawkCodec codec = sparrowhawkCodecFactory.getCodec(job.operation().name());
        job.request().getContext().put(SPARROWHAWK_CODEC, codec);
        if (job.operation().getApiOperation().streamingInput()) {
            SparrowhawkPayloadBufferer bufferer = new SparrowhawkPayloadBufferer();
            ReactiveByteValue value = job.request().getValue();
            Flow.Publisher<ByteBuffer> publisher = value.get();
            publisher.subscribe(bufferer);
            publisher = bufferer;
            CompletableFuture<ByteBuffer> initialEventCallback = new CompletableFuture<>();
            if (sigv4) {
                var headers = job.request().getContext().get(HttpAttributes.HTTP_HEADERS);
                var sigHeaders = headers.map().get("x-amz-content-sha256");
                if (sigHeaders != null && sigHeaders.size() == 1) {
                    var sigHeader = sigHeaders.get(0);
                    if ("STREAMING-AWS4-HMAC-SHA256-EVENTS".equals(sigHeader)) {
                        publisher = new StreamingSigV4Validator(publisher);
                    }
                }
            }
            var initialEventDecoder = new NonBufferingInitialEventDecoder(initialEventCallback);
            publisher.subscribe(initialEventDecoder);
            publisher = initialEventDecoder;
            final var finalPublisher = publisher;
            return initialEventCallback.handle((buffer, e) -> {
                if (e != null) {
                    SerializableStruct failure;
                    if (e instanceof NoInitialEventException) {
                        failure = new software.amazon.smithy.java.server.exceptions.SerializationException(
                            "No initial event received",
                            e
                        );
                    } else if (e instanceof SerializableStruct struct) {
                        failure = struct;
                    } else {
                        failure = new InternalServerException(e);
                    }

                    job.reply().setValue(new ShapeValue<>(failure));
                    job.setDone();
                } else {
                    var mapped = new SparrowhawkStreamDeserializer(finalPublisher, codec);
                    var initialEvent = codec.decodeInitialRequest(buffer, mapped);
                    job.request().setValue(new ShapeValue<>(initialEvent));
                }
                return null;
            });
        } else {
            ByteValue byteValue = job.request().getValue();
            SerializableStruct input;
            try {
                input = codec.decode(ByteBuffer.wrap(byteValue.get()));
            } catch (Exception e) {
                throw new software.amazon.smithy.java.server.exceptions.SerializationException(e);
            }
            job.request().setValue(new ShapeValue<>(input));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static Schema tryResolveExceptionSchema(ModeledApiException me, ApiOperation operation) {
        Schema s = operation.exceptionSchema(me);
        if (s == null) {
            s = SyntheticExceptions.getSchema(me.getShapeId()).orElse(null);
        }
        return s;
    }

    @Override
    public CompletableFuture<Void> serializeOutput(Job job) {
        SparrowhawkCodec codec = job.request().getContext().get(SPARROWHAWK_CODEC);
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("smithy-protocol", List.of("rpc-v2-sparrowhawk"));
        headers.put("Content-Type", List.of("application/vnd.amazon.sparrowhawk"));

        ApiOperation<?, ?> apiOperation = job.operation().getApiOperation();
        ShapeValue<SerializableStruct> value = job.reply().getValue();
        SerializableStruct output = value.get();
        Value<?> response;
        if (output instanceof Exception e) {
            HttpErrorTrait errorTrait;
            Schema schema;
            byte[] serialized;
            if (e instanceof ModeledApiException me && (schema = tryResolveExceptionSchema(me, apiOperation)) != null) {
                errorTrait = schema.getTrait(HttpErrorTrait.class);
                serialized = codec.encodeException(schema, e);
            } else {
                errorTrait = InternalServerException.SCHEMA.getTrait(HttpErrorTrait.class);
                serialized = codec.encodeException(InternalServerException.SCHEMA, new InternalServerException(e));
            }

            response = new ByteValue(serialized);
            if (errorTrait != null) {
                job.reply().context().put(HttpAttributes.STATUS_CODE, errorTrait.getCode());
            }
        } else if (!apiOperation.streamingOutput()) {
            response = new ByteValue(codec.encode(output));
        } else {
            var eventPublisher = codec.getStreamMember(output);
            var initialResponse = ByteBuffer.wrap(codec.encode(output));
            var serializedStream = new SparrowhawkStreamSerializer(eventPublisher, codec);
            var publisher = new Prepender<>(serializedStream, initialResponse);
            response = new ReactiveByteValue(publisher);
        }

        job.reply().setValue(response);
        job.reply().context().put(HttpAttributes.HTTP_HEADERS, HttpHeaders.of(headers, (x, y) -> true));
        return CompletableFuture.completedFuture(null);
    }

    private static final class StreamingSigV4Validator implements Flow.Processor<ByteBuffer, ByteBuffer> {
        private final Flow.Publisher<ByteBuffer> upstream;

        private volatile Flow.Subscription upstreamSubscription;
        private volatile Flow.Subscriber<? super ByteBuffer> downstream;
        private volatile boolean done;

        private StreamingSigV4Validator(Flow.Publisher<ByteBuffer> upstream) {
            this.upstream = upstream;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
            this.downstream = subscriber;
            upstream.subscribe(this);
        }

        @Override
        public void onSubscribe(Flow.Subscription s) {
            this.upstreamSubscription = s;
            this.downstream.onSubscribe(s);
        }

        @Override
        public void onNext(ByteBuffer buffer) {
            try {
                if (done) {
                    // TODO: translate error
                    throw new RuntimeException("Event received after final SigV4 frame");
                }
                SparrowhawkDeserializer d = new SparrowhawkDeserializer(buffer);
                var sigv4Frame = new SparrowhawkSigV4Frame();
                sigv4Frame.decodeFrom(d);
                if (sigv4Frame.hasChunk()) {
                    downstream.onNext(sigv4Frame.getChunk());
                } else {
                    done = true;
                    // since there's nothing left to forward downstream, we have to explicitly request
                    // the completion notification from the request publisher
                    upstreamSubscription.request(1);
                }
            } catch (Exception e) {
                onError(new SerializationException("Malformed event", e));
            }
        }

        @Override
        public void onError(Throwable throwable) {
            downstream.onError(throwable);
        }

        @Override
        public void onComplete() {
            if (!done) {
                // TODO: translate error
                onError(new RuntimeException("Final chunk was never received"));
            }
            downstream.onComplete();
            downstream = null;
        }
    }

    private static final class SparrowhawkStreamDeserializer implements Flow.Processor<ByteBuffer, SerializableStruct> {
        private final Flow.Publisher<ByteBuffer> upstream;
        private final SparrowhawkCodec codec;

        private volatile Flow.Subscription upstreamSubscription;
        private volatile Flow.Subscriber<? super SerializableStruct> downstream;

        SparrowhawkStreamDeserializer(Flow.Publisher<ByteBuffer> upstream, SparrowhawkCodec codec) {
            this.upstream = upstream;
            this.codec = codec;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super SerializableStruct> s) {
            this.downstream = s;
            upstream.subscribe(this);
        }

        @Override
        public void onSubscribe(Flow.Subscription s) {
            this.upstreamSubscription = s;
            this.downstream.onSubscribe(s);
        }

        @Override
        public void onNext(ByteBuffer o) {
            SerializableStruct deserialized;
            try {
                deserialized = codec.deserializeEvent(o);
            } catch (Exception e) {
                upstreamSubscription.cancel();
                // that include a SerializationException as a possible event.
                // TODO: translate error
                downstream.onError(new SerializationException("Malformed event", e));
                return;
            }

            try {
                downstream.onNext(deserialized);
            } catch (Throwable t) {
                upstreamSubscription.cancel();
                onError(t);
            }
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }
    }

    private static final class SparrowhawkStreamSerializer implements Flow.Processor<SerializableStruct, ByteBuffer> {
        private final Flow.Publisher<? extends SerializableStruct> upstream;
        private final SparrowhawkCodec codec;

        private volatile Flow.Subscription upstreamSubscription;
        private volatile Flow.Subscriber<? super ByteBuffer> downstream;

        SparrowhawkStreamSerializer(Flow.Publisher<? extends SerializableStruct> upstream, SparrowhawkCodec codec) {
            this.upstream = upstream;
            this.codec = codec;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super ByteBuffer> s) {
            this.downstream = s;
            upstream.subscribe(this);
        }

        @Override
        public void onSubscribe(Flow.Subscription s) {
            this.upstreamSubscription = s;
            this.downstream.onSubscribe(s);
        }

        @Override
        public void onNext(SerializableStruct o) {
            ByteBuffer serialized;
            try {
                serialized = ByteBuffer.wrap(codec.encodeEvent(o));
            } catch (Exception e) {
                upstreamSubscription.cancel();
                // TODO: translate error
                downstream.onError(new RuntimeException("Malformed event", e));
                return;
            }

            try {
                downstream.onNext(serialized);
            } catch (Throwable t) {
                upstreamSubscription.cancel();
                onError(t);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (t instanceof SerializableStruct s) {
                byte[] serialized;
                try {
                    serialized = codec.encodeEventException(s);
                } catch (Exception e) {
                    downstream.onError(t);
                    return;
                }
                if (serialized == null) {
                    // not an event for this stream
                    downstream.onError(t);
                    return;
                } else {
                    downstream.onNext(ByteBuffer.wrap(serialized));
                    downstream.onComplete();
                }
            } else {
                downstream.onError(t);
            }
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }
    }
}
