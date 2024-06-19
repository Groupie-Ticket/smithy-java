/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.server.protocols.rpcv2.kestrel;

import java.net.http.HttpHeaders;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.java.kestrel.codec.KestrelCodec;
import software.amazon.smithy.java.kestrel.codec.KestrelCodecFactory;
import software.amazon.smithy.java.kestrel.codec.KestrelCodecFactoryIndex;
import software.amazon.smithy.java.runtime.core.Context;
import software.amazon.smithy.java.runtime.core.schema.ApiOperation;
import software.amazon.smithy.java.runtime.core.schema.ModeledApiException;
import software.amazon.smithy.java.runtime.core.schema.Schema;
import software.amazon.smithy.java.runtime.core.schema.SerializableStruct;
import software.amazon.smithy.java.server.Operation;
import software.amazon.smithy.java.server.Service;
import software.amazon.smithy.java.server.core.*;
import software.amazon.smithy.java.server.core.attributes.HttpAttributes;
import software.amazon.smithy.java.server.exceptions.InternalServerException;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.utils.Pair;

final class RpcV2KestrelProtocol extends ServerProtocol {

    private static final Context.Key<KestrelCodec> KESTREL_CODEC = Context.key("kestrelCodec");


    private final KestrelCodecFactory kestrelCodecFactory;

    RpcV2KestrelProtocol(Service service) {
        super(service);
        this.kestrelCodecFactory = KestrelCodecFactoryIndex.getCodecFactory(service.getSchema().getId().toString());
    }

    @Override
    public ShapeId getProtocolId() {
        return ShapeId.from("smithy.protocols#rpcv2Kestrel");
    }

    @Override
    public Operation<?, ?> resolveOperation(ResolutionRequest request) {
        if (!"POST".equals(request.getHttpMethod())) {
            return null;
        }
        List<String> smithyHeaders = request.getHeaders().map().get("smithy-protocol");
        if (smithyHeaders == null || smithyHeaders.size() != 1 || !"rpc-v2-kestrel".equals(smithyHeaders.get(0))) {
            return null;
        }
        String contentType = request.getHeaders().map().get("Content-Type").get(0);
        if (!("application/vnd.amazon.kestrel".equals(contentType) || "application/vnd.amazon.eventstream".equals(
            contentType
        ))) {
            return null;
        }
        Pair<String, String> serviceOperation = RpcV2PathParser.parseRpcV2Path(request.getUri().getPath());
        return getService().getOperation(serviceOperation.right);
    }

    @Override
    public void deserializeInput(Job job) {
        ByteValue byteValue = job.request().getValue();
        KestrelCodec<?, ?, ?, ?> codec = kestrelCodecFactory.getCodec(job.operation().name());
        SerializableStruct input = codec.decode(ByteBuffer.wrap(byteValue.get()));
        job.request().getContext().put(KESTREL_CODEC, codec);
        job.request().setValue(new ShapeValue<>(input));
    }

    @Override
    public void serializeOutput(Job job) {
        KestrelCodec<?, SerializableStruct, ?, ?> codec = job.request().getContext().get(KESTREL_CODEC);
        ApiOperation<?, ?> apiOperation = job.operation().getApiOperation();
        ShapeValue<SerializableStruct> value = job.reply().getValue();
        SerializableStruct output = value.get();
        byte[] serialized;
        HttpErrorTrait errorTrait = null;
        if (output instanceof Exception e) {
            Schema schema;
            if (e instanceof ModeledApiException me && (schema = apiOperation.exceptionSchema(me)) != null) {
                errorTrait = schema.getTrait(HttpErrorTrait.class);
                serialized = codec.encodeException(schema, e);
            } else {
                errorTrait = InternalServerException.SCHEMA.getTrait(HttpErrorTrait.class);
                serialized = codec.encodeException(InternalServerException.SCHEMA, new InternalServerException(e));
            }
        } else {
            serialized = codec.encode(output);
        }
        job.reply().setValue(new ByteValue(serialized));
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("smithy-protocol", List.of("rpc-v2-kestrel"));
        headers.put("Content-Type", List.of("application/vnd.amazon.kestrel"));
        if (errorTrait != null) {
            job.reply().context().put(HttpAttributes.STATUS_CODE, errorTrait.getCode());
        }
        job.reply().context().put(HttpAttributes.HTTP_HEADERS, HttpHeaders.of(headers, (x, y) -> true));
    }
}
