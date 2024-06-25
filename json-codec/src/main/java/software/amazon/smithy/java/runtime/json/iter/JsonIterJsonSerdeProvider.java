package software.amazon.smithy.java.runtime.json.iter;

import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import java.io.IOException;
import java.io.OutputStream;
import software.amazon.smithy.java.runtime.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.runtime.core.serde.ShapeSerializer;
import software.amazon.smithy.java.runtime.json.JsonCodec;
import software.amazon.smithy.java.runtime.json.JsonSerdeProvider;

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
        JsonCodec.Settings settings
    ) {
        return new JsonIterDeserializer(JsonIterator.parse(source), settings, i -> {
            try {
                i.close();
            } catch (IOException ignore) {}
        });
    }

    @Override
    public ShapeSerializer newSerializer(
        OutputStream sink,
        JsonCodec.Settings settings
    ) {
        return new JsonIterSerializer(new JsonStream(sink, 1024), settings, (JsonStream j) -> {
            try {
                j.close();
            } catch (IOException ignore) {}
        });
    }
}
