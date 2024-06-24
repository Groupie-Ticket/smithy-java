package software.amazon.smithy.java.runtime.json.jackson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import software.amazon.smithy.java.runtime.core.serde.SerializationException;
import software.amazon.smithy.java.runtime.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.runtime.core.serde.ShapeSerializer;
import software.amazon.smithy.java.runtime.json.JsonFieldMapper;
import software.amazon.smithy.java.runtime.json.JsonSerdeProvider;
import software.amazon.smithy.java.runtime.json.TimestampResolver;

public class JacksonJsonSerdeProvider implements JsonSerdeProvider {

    private static final JsonFactory FACTORY = new ObjectMapper().getFactory();

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public String getName() {
        return "jackson";
    }

    @Override
    public ShapeDeserializer newDeserializer(
        byte[] source,
        JsonFieldMapper fieldMapper,
        TimestampResolver timestampResolver
    ) {
        try {
            return new JacksonJsonDeserializer(FACTORY.createParser(source), fieldMapper, timestampResolver);
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public ShapeSerializer newSerializer(
        OutputStream sink,
        JsonFieldMapper fieldMapper,
        TimestampResolver timestampResolver
    ) {
        try {
            return new JacksonJsonSerializer(FACTORY.createGenerator(sink), fieldMapper, timestampResolver);
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }
}
