package software.amazon.smithy.java.runtime.json.iter;

import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import java.io.IOException;
import java.io.OutputStream;
import software.amazon.smithy.java.runtime.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.runtime.core.serde.ShapeSerializer;
import software.amazon.smithy.java.runtime.json.JsonFieldMapper;
import software.amazon.smithy.java.runtime.json.JsonSerdeProvider;
import software.amazon.smithy.java.runtime.json.TimestampResolver;

public final class JsonIterJsonSerdeProvider implements JsonSerdeProvider {
    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public String getName() {
        return "json-iter";
    }

    @Override
    public ShapeDeserializer newDeserializer(
        byte[] source,
        JsonFieldMapper fieldMapper,
        TimestampResolver timestampResolver
    ) {
        return new JsonIterDeserializer(JsonIterator.parse(source), timestampResolver, fieldMapper, i -> {
            try {
                i.close();
            } catch (IOException ignore) {}
        });
    }

    @Override
    public ShapeSerializer newSerializer(
        OutputStream sink,
        JsonFieldMapper fieldMapper,
        TimestampResolver timestampResolver
    ) {
        return new JsonIterSerializer(new JsonStream(sink, 1024), fieldMapper, timestampResolver, (JsonStream j) -> {
            try {
                j.close();
            } catch (IOException ignore) {}
        });
    }
}
