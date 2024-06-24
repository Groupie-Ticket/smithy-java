/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.runtime.json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.java.runtime.core.schema.PreludeSchemas;
import software.amazon.smithy.java.runtime.core.schema.Schema;
import software.amazon.smithy.java.runtime.core.serde.SerializationException;
import software.amazon.smithy.java.runtime.core.serde.TimestampFormatter;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

public class JsonDeserializerTest {
    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void deserializesByte(JsonCodec.Builder builder) {
        try (var codec = builder.build()) {
            var de = codec.createDeserializer("1".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readByte(PreludeSchemas.BYTE), is((byte) 1));
        }
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void deserializesShort(JsonCodec.Builder builder) {
        try (var codec = builder.build()) {
            var de = codec.createDeserializer("1".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readShort(PreludeSchemas.SHORT), is((short) 1));
        }
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void deserializesInteger(JsonCodec.Builder builder) {
        try (var codec = builder.build()) {
            var de = codec.createDeserializer("1".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readInteger(PreludeSchemas.INTEGER), is(1));
        }
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void deserializesLong(JsonCodec.Builder builder) {
        try (var codec = builder.build()) {
            var de = codec.createDeserializer("1".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readLong(PreludeSchemas.LONG), is(1L));
        }
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void deserializesFloat(JsonCodec.Builder builder) {
        try (var codec = builder.build()) {
            var de = codec.createDeserializer("1".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readFloat(PreludeSchemas.FLOAT), is(1.0f));
            de = codec.createDeserializer("\"NaN\"".getBytes(StandardCharsets.UTF_8));
            assertTrue(Float.isNaN(de.readFloat(PreludeSchemas.FLOAT)));
            de = codec.createDeserializer("\"Infinity\"".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readFloat(PreludeSchemas.FLOAT), is(Float.POSITIVE_INFINITY));
            de = codec.createDeserializer("\"-Infinity\"".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readFloat(PreludeSchemas.FLOAT), is(Float.NEGATIVE_INFINITY));
        }
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void deserializesDouble(JsonCodec.Builder builder) {
        try (var codec = builder.build()) {
            var de = codec.createDeserializer("1".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readDouble(PreludeSchemas.DOUBLE), is(1.0));
            de = codec.createDeserializer("\"NaN\"".getBytes(StandardCharsets.UTF_8));
            assertTrue(Double.isNaN(de.readDouble(PreludeSchemas.DOUBLE)));
            de = codec.createDeserializer("\"Infinity\"".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readDouble(PreludeSchemas.DOUBLE), is(Double.POSITIVE_INFINITY));
            de = codec.createDeserializer("\"-Infinity\"".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readDouble(PreludeSchemas.DOUBLE), is(Double.NEGATIVE_INFINITY));
        }
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void deserializesBigInteger(JsonCodec.Builder builder) {
        try (var codec = builder.build()) {
            var de = codec.createDeserializer("1".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readBigInteger(PreludeSchemas.BIG_INTEGER), is(BigInteger.ONE));
        }
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void deserializesBigIntegerOnlyFromRawNumbersByDefault(JsonCodec.Builder builder) {
        try (var codec = builder.build()) {
            var de = codec.createDeserializer("\"1\"".getBytes(StandardCharsets.UTF_8));
            Assertions.assertThrows(SerializationException.class, () -> de.readBigInteger(PreludeSchemas.BIG_INTEGER));
        }
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void deserializesBigDecimal(JsonCodec.Builder builder) {
        try (var codec = builder.build()) {
            var de = codec.createDeserializer("1".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readBigDecimal(PreludeSchemas.BIG_DECIMAL), is(BigDecimal.ONE));
        }
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void deserializesBigDecimalOnlyFromRawNumbersByDefault(JsonCodec.Builder builder) {
        try (var codec = builder.build()) {
            var de = codec.createDeserializer("\"1\"".getBytes(StandardCharsets.UTF_8));
            Assertions.assertThrows(SerializationException.class, () -> de.readBigDecimal(PreludeSchemas.BIG_DECIMAL));
        }
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void deserializesTimestamp(JsonCodec.Builder builder) {
        try (var codec = builder.build()) {
            var sink = new ByteArrayOutputStream();
            try (var ser = codec.createSerializer(sink)) {
                ser.writeTimestamp(PreludeSchemas.TIMESTAMP, Instant.EPOCH);
            }

            var de = codec.createDeserializer(sink.toByteArray());
            assertThat(de.readTimestamp(PreludeSchemas.TIMESTAMP), equalTo(Instant.EPOCH));
        }
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void deserializesBlob(JsonCodec.Builder builder) {
        try (var codec = builder.build()) {
            var str = "foo";
            var expected = Base64.getEncoder().encodeToString(str.getBytes());
            var de = codec.createDeserializer(("\"" + expected + "\"").getBytes(StandardCharsets.UTF_8));
            assertThat(de.readBlob(PreludeSchemas.BLOB), equalTo(str.getBytes(StandardCharsets.UTF_8)));
        }
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void deserializesBoolean(JsonCodec.Builder builder) {
        try (var codec = builder.build()) {
            var de = codec.createDeserializer("true".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readBoolean(PreludeSchemas.BOOLEAN), is(true));
        }
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void deserializesString(JsonCodec.Builder builder) {
        try (var codec = builder.build()) {
            var de = codec.createDeserializer("\"foo\"".getBytes(StandardCharsets.UTF_8));
            assertThat(de.readString(PreludeSchemas.STRING), equalTo("foo"));
        }
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void deserializesList(JsonCodec.Builder builder) {
        try (var codec = builder.build()) {
            var de = codec.createDeserializer("[\"foo\",\"bar\"]".getBytes(StandardCharsets.UTF_8));
            List<String> values = new ArrayList<>();

            de.readList(PreludeSchemas.DOCUMENT, null, (ignore, firstList) -> {
                values.add(firstList.readString(PreludeSchemas.STRING));
            });

            assertThat(values, equalTo(List.of("foo", "bar")));
        }
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void deserializesMap(JsonCodec.Builder builder) {
        try (var codec = builder.build()) {
            var de = codec.createDeserializer("{\"foo\":\"bar\",\"baz\":\"bam\"}".getBytes(StandardCharsets.UTF_8));
            Map<String, String> result = new LinkedHashMap<>();

            de.readStringMap(PreludeSchemas.DOCUMENT, result, (map, key, mapde) -> {
                map.put(key, mapde.readString(PreludeSchemas.STRING));
            });

            assertThat(result.values(), hasSize(2));
            assertThat(result, hasKey("foo"));
            assertThat(result, hasKey("baz"));
            assertThat(result.get("foo"), equalTo("bar"));
            assertThat(result.get("baz"), equalTo("bam"));
        }
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void deserializesStruct(JsonCodec.Builder builder) {
        try (var codec = builder.useJsonName(true).build()) {
            var de = codec.createDeserializer("{\"name\":\"Sam\",\"Color\":\"red\"}".getBytes(StandardCharsets.UTF_8));
            Set<String> members = new LinkedHashSet<>();

            de.readStruct(JsonTestData.BIRD, members, (memberResult, member, deser) -> {
                memberResult.add(member.memberName());
                switch (member.memberName()) {
                    case "name" -> assertThat(deser.readString(JsonTestData.BIRD.member("name")), equalTo("Sam"));
                    case "color" -> assertThat(deser.readString(JsonTestData.BIRD.member("color")), equalTo("red"));
                    default -> throw new IllegalStateException("Unexpected member: " + member);
                }
            });

            assertThat(members, contains("name", "color"));
        }
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void skipsUnknownMembers(JsonCodec.Builder builder) {
        try (var codec = builder.useJsonName(true).build()) {
            var de = codec.createDeserializer(
                "{\"name\":\"Sam\",\"Ignore\":[1,2,3],\"Color\":\"rainbow\"}".getBytes(StandardCharsets.UTF_8)
            );
            Set<String> members = new LinkedHashSet<>();

            de.readStruct(JsonTestData.BIRD, members, (memberResult, member, deser) -> {
                memberResult.add(member.memberName());
                switch (member.memberName()) {
                    case "name" -> assertThat(deser.readString(JsonTestData.BIRD.member("name")), equalTo("Sam"));
                    case "color" -> assertThat(deser.readString(JsonTestData.BIRD.member("color")), equalTo("rainbow"));
                    default -> throw new IllegalStateException("Unexpected member: " + member);
                }
            });

            assertThat(members, contains("name", "color"));
        }
    }

    @ParameterizedTest
    @MethodSource("deserializesBirdWithJsonNameOrNotSource")
    public void deserializesBirdWithJsonNameOrNot(boolean useJsonName, String input, JsonCodec.Builder builder) {
        try (var codec = builder.useJsonName(useJsonName).build()) {
            var de = codec.createDeserializer(input.getBytes(StandardCharsets.UTF_8));
            Set<String> members = new LinkedHashSet<>();
            de.readStruct(JsonTestData.BIRD, members, (memberResult, member, deser) -> {
                memberResult.add(member.memberName());
                switch (member.memberName()) {
                    case "name" -> assertThat(deser.readString(JsonTestData.BIRD.member("name")), equalTo("Sam"));
                    case "color" -> assertThat(deser.readString(JsonTestData.BIRD.member("color")), equalTo("red"));
                    default -> throw new IllegalStateException("Unexpected member: " + member);
                }
            });
            assertThat(members, contains("name", "color"));
        }
    }

    public static List<Arguments> deserializesBirdWithJsonNameOrNotSource() {
        return JsonTestData.builders()
            .flatMap(
                a -> Stream.of(
                    Arguments.of(true, "{\"name\":\"Sam\",\"Color\":\"red\"}", a.get()[0]),
                    Arguments.of(false, "{\"name\":\"Sam\",\"color\":\"red\"}", a.get()[0])
                )
            )
            .toList();
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void readsDocuments(JsonCodec.Builder builder) {
        var json = "{\"name\":\"Sam\",\"color\":\"red\"}".getBytes(StandardCharsets.UTF_8);

        try (var codec = builder.build()) {
            var de = codec.createDeserializer(json);
            var document = de.readDocument();

            assertThat(document.type(), is(ShapeType.MAP));
            var map = document.asStringMap();
            assertThat(map.values(), hasSize(2));
            assertThat(map.get("name").asString(), equalTo("Sam"));
            assertThat(map.get("color").asString(), equalTo("red"));
        }
    }

    @ParameterizedTest
    @MethodSource("deserializesWithTimestampFormatSource")
    public void deserializesWithTimestampFormat(
        boolean useTrait,
        TimestampFormatTrait trait,
        TimestampFormatter defaultFormat,
        String json
    ) {
        Schema.Builder schemaBuilder = Schema.builder()
            .type(ShapeType.TIMESTAMP)
            .id("smithy.foo#Time");

        if (trait != null) {
            schemaBuilder.traits(trait);
        }

        var schema = schemaBuilder.build();
        var codecBuilder = JsonCodec.builder().useTimestampFormat(useTrait);
        if (defaultFormat != null) {
            codecBuilder.defaultTimestampFormat(defaultFormat);
        }

        try (var codec = codecBuilder.build()) {
            var de = codec.createDeserializer(json.getBytes(StandardCharsets.UTF_8));
            assertThat(de.readTimestamp(schema), equalTo(Instant.EPOCH));
        }
    }

    public static List<Arguments> deserializesWithTimestampFormatSource() {
        var epochSeconds = Double.toString(((double) Instant.EPOCH.toEpochMilli()) / 1000);

        return JsonTestData.builders()
            .flatMap(
                a -> Stream.of(
                    // boolean useTrait, TimestampFormatTrait trait, TimestampFormatter defaultFormat, String json
                    Arguments.of(false, null, null, epochSeconds, a.get()[0]),
                    Arguments.of(
                        false,
                        new TimestampFormatTrait(TimestampFormatTrait.EPOCH_SECONDS),
                        null,
                        epochSeconds,
                        a.get()[0]
                    ),
                    Arguments.of(
                        false,
                        new TimestampFormatTrait(TimestampFormatTrait.EPOCH_SECONDS),
                        TimestampFormatter.Prelude.EPOCH_SECONDS,
                        epochSeconds,
                        a.get()[0]
                    ),
                    Arguments.of(
                        true,
                        new TimestampFormatTrait(TimestampFormatTrait.EPOCH_SECONDS),
                        TimestampFormatter.Prelude.EPOCH_SECONDS,
                        epochSeconds,
                        a.get()[0]
                    ),
                    Arguments.of(
                        true,
                        new TimestampFormatTrait(TimestampFormatTrait.EPOCH_SECONDS),
                        null,
                        epochSeconds,
                        a.get()[0]
                    ),
                    Arguments.of(
                        true,
                        new TimestampFormatTrait(TimestampFormatTrait.EPOCH_SECONDS),
                        TimestampFormatter.Prelude.DATE_TIME,
                        epochSeconds,
                        a.get()[0]
                    ),
                    Arguments.of(
                        false,
                        new TimestampFormatTrait(TimestampFormatTrait.EPOCH_SECONDS),
                        TimestampFormatter.Prelude.DATE_TIME,
                        "\"" + Instant.EPOCH + "\"",
                        a.get()[0]
                    ),
                    Arguments.of(
                        true,
                        new TimestampFormatTrait(TimestampFormatTrait.DATE_TIME),
                        TimestampFormatter.Prelude.EPOCH_SECONDS,
                        "\"" + Instant.EPOCH + "\"",
                        a.get()[0]
                    )
                )
            )
            .toList();
    }

    @ParameterizedTest
    @MethodSource("software.amazon.smithy.java.runtime.json.JsonTestData#builders")
    public void throwsWhenTimestampIsWrongType(JsonCodec.Builder builder) {
        Schema schema = Schema.builder()
            .type(ShapeType.TIMESTAMP)
            .id("smithy.foo#Time")
            .build();

        try (var codec = builder.build()) {
            var de = codec.createDeserializer("true".getBytes(StandardCharsets.UTF_8));
            var e = Assertions.assertThrows(SerializationException.class, () -> de.readTimestamp(schema));
            assertThat(e.getMessage(), equalTo("Expected a timestamp, but found boolean"));
        }
    }
}
