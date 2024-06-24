/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.java.runtime.core.schema.PreludeSchemas;
import software.amazon.smithy.java.runtime.core.schema.Schema;
import software.amazon.smithy.java.runtime.core.schema.SerializableShape;
import software.amazon.smithy.java.runtime.core.schema.ShapeBuilder;
import software.amazon.smithy.java.runtime.core.serde.SerializationException;
import software.amazon.smithy.java.runtime.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.runtime.core.serde.ShapeSerializer;
import software.amazon.smithy.java.runtime.core.serde.TimestampFormatter;
import software.amazon.smithy.java.runtime.core.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

public class JsonDocumentTest {

    private static final String FOO_B64 = "Zm9v";

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void convertsNumberToNumber(JsonCodec.Builder builder) {
        var codec = builder.build();
        var de = codec.createDeserializer("120".getBytes(StandardCharsets.UTF_8));

        var document = de.readDocument();
        assertThat(document.type(), is(ShapeType.INTEGER));
        assertThat(document.asByte(), is((byte) 120));
        assertThat(document.asShort(), is((short) 120));
        assertThat(document.asInteger(), is(120));
        assertThat(document.asLong(), is(120L));
        assertThat(document.asFloat(), is(120f));
        assertThat(document.asDouble(), is(120.0));
        assertThat(document.asBigInteger(), equalTo(BigInteger.valueOf(120)));
        assertThat(document.asBigDecimal(), comparesEqualTo(BigDecimal.valueOf(120.0)));
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void convertsDoubleToNumber(JsonCodec.Builder builder) {
        var codec = builder.build();
        var de = codec.createDeserializer("1.1".getBytes(StandardCharsets.UTF_8));

        var document = de.readDocument();
        assertThat(document.type(), is(ShapeType.DOUBLE));
        assertThat(document.asFloat(), is(1.1f));
        assertThat(document.asDouble(), is(1.1));
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void convertsToBoolean(JsonCodec.Builder builder) {
        var codec = builder.build();
        var de = codec.createDeserializer("true".getBytes(StandardCharsets.UTF_8));

        var document = de.readDocument();
        assertThat(document.type(), is(ShapeType.BOOLEAN));
        assertThat(document.asBoolean(), is(true));
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void convertsToTimestampWithEpochSeconds(JsonCodec.Builder builder) {
        var codec = builder.build();
        var de = codec.createDeserializer("0".getBytes(StandardCharsets.UTF_8));

        var document = de.readDocument();
        assertThat(document.type(), is(ShapeType.INTEGER));
        assertThat(document.asTimestamp(), equalTo(Instant.EPOCH));
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void convertsToTimestampWithDefaultStringFormat(JsonCodec.Builder builder) {
        var now = Instant.now();
        var codec = builder.defaultTimestampFormat(TimestampFormatter.Prelude.DATE_TIME).build();
        var de = codec.createDeserializer(("\"" + now + "\"").getBytes(StandardCharsets.UTF_8));

        var document = de.readDocument();
        assertThat(document.type(), is(ShapeType.STRING));
        assertThat(document.asTimestamp(), equalTo(now));
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void convertsToTimestampFailsOnUnknownType(JsonCodec.Builder builder) {
        var codec = builder.build();
        var de = codec.createDeserializer("true".getBytes(StandardCharsets.UTF_8));
        var document = de.readDocument();

        var e = Assertions.assertThrows(SerializationException.class, document::asTimestamp);
        assertThat(e.getMessage(), containsString("Expected a timestamp, but found boolean"));
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void convertsToBlob(JsonCodec.Builder builder) {
        var codec = builder.build();
        var de = codec.createDeserializer(("\"" + FOO_B64 + "\"").getBytes(StandardCharsets.UTF_8));
        var document = de.readDocument();

        assertThat(document.type(), is(ShapeType.STRING));

        // Reading here as a blob will base64 decode the value.
        assertThat(document.asBlob(), equalTo("foo".getBytes(StandardCharsets.UTF_8)));
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void convertsToList(JsonCodec.Builder builder) {
        var codec = builder.build();
        var de = codec.createDeserializer("[1, 2, 3]".getBytes(StandardCharsets.UTF_8));

        var document = de.readDocument();
        assertThat(document.type(), is(ShapeType.LIST));

        var list = document.asList();
        assertThat(list, hasSize(3));
        assertThat(list.get(0).type(), is(ShapeType.INTEGER));
        assertThat(list.get(0).asInteger(), is(1));
        assertThat(list.get(1).asInteger(), is(2));
        assertThat(list.get(2).asInteger(), is(3));
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void convertsToMap(JsonCodec.Builder builder) {
        var codec = builder.build();
        var de = codec.createDeserializer("{\"a\":1,\"b\":true}".getBytes(StandardCharsets.UTF_8));

        var document = de.readDocument();
        assertThat(document.type(), is(ShapeType.MAP));

        var map = document.asStringMap();
        assertThat(map.keySet(), hasSize(2));
        assertThat(map.get("a").type(), is(ShapeType.INTEGER));
        assertThat(map.get("a").asInteger(), is(1));
        assertThat(document.getMember("a").type(), is(ShapeType.INTEGER));

        assertThat(map.get("b").type(), is(ShapeType.BOOLEAN));
        assertThat(map.get("b").asBoolean(), is(true));
        assertThat(document.getMember("b").type(), is(ShapeType.BOOLEAN));
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void nullAndMissingMapMembersReturnsNull(JsonCodec.Builder builder) {
        var codec = builder.build();
        var de = codec.createDeserializer("{\"a\":null}".getBytes(StandardCharsets.UTF_8));

        var document = de.readDocument();
        assertThat(document.getMember("c"), nullValue());
        assertThat(document.getMember("d"), nullValue());
    }

    @ParameterizedTest
    @MethodSource("failToConvertSource")
    public void failToConvert(String json, Consumer<Document> consumer, JsonCodec.Builder builder) {
        var codec = builder.build();
        var de = codec.createDeserializer(json.getBytes(StandardCharsets.UTF_8));
        var document = de.readDocument();

        Assertions.assertThrows(SerializationException.class, () -> consumer.accept(document));
    }

    public static List<Arguments> failToConvertSource() {
        return JsonTestData.builders()
            .flatMap(
                a -> Stream.of(
                    Arguments.of("1", (Consumer<Document>) Document::asBoolean, a.get()[0]),
                    Arguments.of("1", (Consumer<Document>) Document::asBlob, a.get()[0]),
                    Arguments.of("1", (Consumer<Document>) Document::asString, a.get()[0]),
                    Arguments.of("1", (Consumer<Document>) Document::asList, a.get()[0]),
                    Arguments.of("1", (Consumer<Document>) Document::asStringMap, a.get()[0]),
                    Arguments.of("1", (Consumer<Document>) Document::asBlob, a.get()[0]),

                    Arguments.of("\"1\"", (Consumer<Document>) Document::asBoolean, a.get()[0]),
                    Arguments.of("\"1\"", (Consumer<Document>) Document::asList, a.get()[0]),
                    Arguments.of("\"1\"", (Consumer<Document>) Document::asStringMap, a.get()[0]),
                    Arguments.of("\"1\"", (Consumer<Document>) Document::asBlob, a.get()[0]),
                    Arguments.of("\"1\"", (Consumer<Document>) Document::asBoolean, a.get()[0]),
                    Arguments.of("\"1\"", (Consumer<Document>) Document::asByte, a.get()[0]),
                    Arguments.of("\"1\"", (Consumer<Document>) Document::asShort, a.get()[0]),
                    Arguments.of("\"1\"", (Consumer<Document>) Document::asInteger, a.get()[0]),
                    Arguments.of("\"1\"", (Consumer<Document>) Document::asLong, a.get()[0]),
                    Arguments.of("\"1\"", (Consumer<Document>) Document::asFloat, a.get()[0]),
                    Arguments.of("\"1\"", (Consumer<Document>) Document::asDouble, a.get()[0]),
                    Arguments.of("\"1\"", (Consumer<Document>) Document::asBigInteger, a.get()[0]),
                    Arguments.of("\"1\"", (Consumer<Document>) Document::asBigDecimal, a.get()[0])
                )
            )
            .toList();
    }

    @ParameterizedTest
    @MethodSource("serializeContentSource")
    public void serializeContent(String json, JsonCodec.Builder builder) {
        var codec = builder.build();
        var sink = new ByteArrayOutputStream();
        var se = codec.createSerializer(sink);
        var de = codec.createDeserializer(json.getBytes(StandardCharsets.UTF_8));
        var document = de.readDocument();
        document.serializeContents(se);
        se.flush();

        assertThat(sink.toString(StandardCharsets.UTF_8), equalTo(json));
    }

    @ParameterizedTest
    @MethodSource("serializeContentSource")
    public void serializeDocument(String json, JsonCodec.Builder builder) {
        var codec = builder.build();
        var sink = new ByteArrayOutputStream();
        var se = codec.createSerializer(sink);
        var de = codec.createDeserializer(json.getBytes(StandardCharsets.UTF_8));
        var document = de.readDocument();
        document.serialize(se);
        se.flush();

        assertThat(sink.toString(StandardCharsets.UTF_8), equalTo(json));
    }

    public static List<Arguments> serializeContentSource() {
        return JsonTestData.builders()
            .flatMap(
                a -> Stream.of(
                    Arguments.of("true", a.get()[0]),
                    Arguments.of("false", a.get()[0]),
                    Arguments.of("1", a.get()[0]),
                    Arguments.of("1.1", a.get()[0]),
                    Arguments.of("[1,2,3]", a.get()[0]),
                    Arguments.of("{\"a\":1,\"b\":[1,true,-20,\"hello\"]}", a.get()[0])
                )
            )
            .toList();
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void deserializesIntoBuilderWithJsonNameAndTimestampFormat(JsonCodec.Builder codecBuilder) {
        String date = Instant.EPOCH.toString();
        var json = "{\"name\":\"Hank\",\"BINARY\":\"" + FOO_B64 + "\",\"date\":\"" + date + "\",\"numbers\":[1,2,3]}";
        var codec = codecBuilder.useTimestampFormat(true).useJsonName(true).build();
        var de = codec.createDeserializer(json.getBytes(StandardCharsets.UTF_8));
        var document = de.readDocument();

        var builder = new TestPojo.Builder();
        document.deserializeInto(builder);
        var pojo = builder.build();

        assertThat(pojo.name, equalTo("Hank"));
        assertThat(pojo.binary, equalTo("foo".getBytes(StandardCharsets.UTF_8)));
        assertThat(pojo.date, equalTo(Instant.EPOCH));
        assertThat(pojo.numbers, equalTo(List.of(1, 2, 3)));
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void deserializesIntoBuilder(JsonCodec.Builder codecBuilder) {
        var json = "{\"name\":\"Hank\",\"binary\":\"" + FOO_B64 + "\",\"date\":0,\"numbers\":[1,2,3]}";
        var codec = codecBuilder.build();
        var de = codec.createDeserializer(json.getBytes(StandardCharsets.UTF_8));
        var document = de.readDocument();

        var builder = new TestPojo.Builder();
        document.deserializeInto(builder);
        var pojo = builder.build();

        assertThat(pojo.name, equalTo("Hank"));
        assertThat(pojo.binary, equalTo("foo".getBytes(StandardCharsets.UTF_8)));
        assertThat(pojo.date, equalTo(Instant.EPOCH));
        assertThat(pojo.numbers, equalTo(List.of(1, 2, 3)));
    }

    private static final class TestPojo implements SerializableShape {

        private static final ShapeId ID = ShapeId.from("smithy.example#Foo");

        private static final Schema NAME = Schema.memberBuilder("name", PreludeSchemas.STRING)
            .id(ID)
            .build();

        private static final Schema BINARY = Schema.memberBuilder("binary", PreludeSchemas.BLOB)
            .id(ID)
            .traits(new JsonNameTrait("BINARY"))
            .build();

        private static final Schema DATE = Schema.memberBuilder("date", PreludeSchemas.TIMESTAMP)
            .id(ID)
            .traits(new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME))
            .build();

        private static final Schema NUMBERS_LIST = Schema.builder()
            .type(ShapeType.LIST)
            .id("smithy.example#Numbers")
            .members(Schema.memberBuilder("member", PreludeSchemas.INTEGER))
            .build();

        private static final Schema NUMBERS = Schema.memberBuilder("numbers", NUMBERS_LIST)
            .id(ID)
            .build();

        private static final Schema SCHEMA = Schema.builder()
            .id(ID)
            .type(ShapeType.STRUCTURE)
            .members(NAME, BINARY, DATE, NUMBERS)
            .build();

        private final String name;
        private final byte[] binary;
        private final Instant date;
        private final List<Integer> numbers;

        TestPojo(Builder builder) {
            this.name = builder.name;
            this.binary = builder.binary;
            this.date = builder.date;
            this.numbers = builder.numbers;
        }

        @Override
        public void serialize(ShapeSerializer encoder) {
            throw new UnsupportedOperationException();
        }

        private static final class Builder implements ShapeBuilder<TestPojo> {

            private String name;
            private byte[] binary;
            private Instant date;
            private final List<Integer> numbers = new ArrayList<>();

            @Override
            public Builder deserialize(ShapeDeserializer decoder) {
                decoder.readStruct(SCHEMA, this, (pojo, member, deser) -> {
                    switch (member.memberName()) {
                        case "name" -> pojo.name = deser.readString(NAME);
                        case "binary" -> pojo.binary = deser.readBlob(BINARY);
                        case "date" -> pojo.date = deser.readTimestamp(DATE);
                        case "numbers" -> {
                            deser.readList(NUMBERS, pojo.numbers, (values, de) -> {
                                values.add(de.readInteger(NUMBERS_LIST.member("member")));
                            });
                        }
                        default -> throw new UnsupportedOperationException(member.toString());
                    }
                });
                return this;
            }

            @Override
            public TestPojo build() {
                return new TestPojo(this);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("checkEqualitySource")
    public void checkEquality(String left, String right, boolean equal, JsonCodec.Builder builder) {
        var codec = builder.build();

        var de1 = codec.createDeserializer(left.getBytes(StandardCharsets.UTF_8));
        var leftValue = de1.readDocument();

        var de2 = codec.createDeserializer(right.getBytes(StandardCharsets.UTF_8));
        var rightValue = de2.readDocument();

        assertThat(leftValue.equals(rightValue), is(equal));
    }

    public static List<Arguments> checkEqualitySource() {
        return JsonTestData.builders()
            .flatMap(
                a -> Stream.of(
                    Arguments.of("1", "1", true, a.get()[0]),
                    Arguments.of("1", "1.1", false, a.get()[0]),
                    Arguments.of("true", "true", true, a.get()[0]),
                    Arguments.of("true", "false", false, a.get()[0]),
                    Arguments.of("1", "false", false, a.get()[0]),
                    Arguments.of("1", "\"1\"", false, a.get()[0]),
                    Arguments.of("\"foo\"", "\"foo\"", true, a.get()[0]),
                    Arguments.of("[\"foo\"]", "[\"foo\"]", true, a.get()[0]),
                    Arguments.of("{\"foo\":\"foo\"}", "{\"foo\":\"foo\"}", true, a.get()[0]),
                    Arguments.of("{\"foo\":\"foo\"}", "{\"foo\":\"bar\"}", false, a.get()[0])
                )
            )
            .toList();
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void onlyEqualIfBothUseTimestampFormat(JsonCodec.Builder builder) {
        var de1 = builder
            .useTimestampFormat(true)
            .build()
            .createDeserializer("1".getBytes(StandardCharsets.UTF_8));
        var de2 = builder
            .useTimestampFormat(false)
            .build()
            .createDeserializer("1".getBytes(StandardCharsets.UTF_8));

        var leftValue = de1.readDocument();
        var rightValue = de2.readDocument();

        assertThat(leftValue, not(equalTo(rightValue)));
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void onlyEqualIfBothUseJsonName(JsonCodec.Builder builder) {
        var de1 = builder
            .useJsonName(true)
            .build()
            .createDeserializer("1".getBytes(StandardCharsets.UTF_8));
        var de2 = builder
            .useJsonName(false)
            .build()
            .createDeserializer("1".getBytes(StandardCharsets.UTF_8));

        var leftValue = de1.readDocument();
        var rightValue = de2.readDocument();

        assertThat(leftValue, not(equalTo(rightValue)));
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void canNormalizeJsonDocuments(JsonCodec.Builder builder) {
        var codec = builder.build();
        var de = codec.createDeserializer("true".getBytes(StandardCharsets.UTF_8));
        var json = de.readDocument();

        assertThat(json.normalize(), equalTo(Document.createBoolean(true)));
    }
}
