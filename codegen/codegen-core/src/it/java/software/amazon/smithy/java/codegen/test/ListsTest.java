/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.test;

import static java.nio.ByteBuffer.wrap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.java.codegen.test.model.ListAllTypesInput;
import software.amazon.smithy.java.codegen.test.model.NestedEnum;
import software.amazon.smithy.java.codegen.test.model.NestedIntEnum;
import software.amazon.smithy.java.codegen.test.model.NestedListsInput;
import software.amazon.smithy.java.codegen.test.model.NestedStruct;
import software.amazon.smithy.java.codegen.test.model.NestedUnion;
import software.amazon.smithy.java.codegen.test.model.SetsAllTypesInput;
import software.amazon.smithy.java.codegen.test.model.SparseListsInput;
import software.amazon.smithy.java.core.schema.SerializableShape;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.utils.ListUtils;

public class ListsTest {

    static Stream<SerializableShape> listTypes() {
        return Stream.of(
                ListAllTypesInput.builder()
                        .listOfBoolean(
                                List.of(true, true, false))
                        .build(),
                ListAllTypesInput.builder()
                        .listOfBigDecimal(
                                List.of(BigDecimal.TEN, BigDecimal.ZERO))
                        .build(),
                ListAllTypesInput.builder()
                        .listOfBigInteger(
                                List.of(BigInteger.TEN, BigInteger.ZERO))
                        .build(),
                ListAllTypesInput.builder()
                        .listOfByte(
                                List.of((byte) 1, (byte) 2))
                        .build(),
                ListAllTypesInput.builder()
                        .listOfDouble(
                                List.of(2.0, 3.0))
                        .build(),
                ListAllTypesInput.builder()
                        .listOfFloat(
                                List.of(2f, 3f))
                        .build(),
                ListAllTypesInput.builder()
                        .listOfInteger(
                                List.of(1, 2))
                        .build(),
                ListAllTypesInput.builder()
                        .listOfLong(
                                List.of(1L, 2L))
                        .build(),
                ListAllTypesInput.builder()
                        .listOfShort(
                                List.of((short) 1, (short) 2))
                        .build(),
                ListAllTypesInput.builder()
                        .listOfString(
                                List.of("a", "b"))
                        .build(),
                ListAllTypesInput.builder()
                        .listOfBlobs(
                                Stream.of(Base64.getDecoder().decode("YmxvYg=="),
                                        Base64.getDecoder().decode("YmlyZHM="))
                                        .map(ByteBuffer::wrap)
                                        .toList())
                        .build(),
                ListAllTypesInput.builder()
                        .listOfTimestamps(
                                List.of(Instant.EPOCH, Instant.MIN))
                        .build(),
                ListAllTypesInput.builder()
                        .listOfUnion(
                                List.of(new NestedUnion.AMember("string"), new NestedUnion.BMember(2)))
                        .build(),
                ListAllTypesInput.builder()
                        .listOfEnum(
                                List.of(NestedEnum.A, NestedEnum.B))
                        .build(),
                ListAllTypesInput.builder()
                        .listOfIntEnum(
                                List.of(NestedIntEnum.A, NestedIntEnum.B))
                        .build(),
                ListAllTypesInput.builder()
                        .listOfStruct(
                                List.of(NestedStruct.builder().build(), NestedStruct.builder().build()))
                        .build(),
                ListAllTypesInput.builder()
                        .listOfDocuments(
                                List.of(Document.of(2.0), Document.of("string")))
                        .build());
    }

    static Stream<SerializableShape> nestedLists() {
        return Stream.of(
                NestedListsInput.builder()
                        .listOfLists(
                                List.of(List.of("a", "b"), List.of("c", "d")))
                        .build(),
                NestedListsInput.builder()
                        .listOfListOfList(
                                List.of(List.of(List.of("a", "b"), List.of("c", "d"))))
                        .build(),
                NestedListsInput.builder()
                        .listOfMaps(
                                List.of(Map.of("a", "b"), Map.of("c", "d")))
                        .build());
    }

    static Stream<SerializableShape> sparseLists() {
        return Stream.of(
                SparseListsInput.builder()
                        .listOfBooleans(
                                ListUtils.of(true, true, null, false))
                        .build(),
                SparseListsInput.builder()
                        .listOfBigDecimal(
                                ListUtils.of(BigDecimal.TEN, null, BigDecimal.ZERO))
                        .build(),
                SparseListsInput.builder()
                        .listOfBigInteger(
                                ListUtils.of(BigInteger.TEN, null, BigInteger.ZERO))
                        .build(),
                SparseListsInput.builder()
                        .listOfByte(
                                ListUtils.of((byte) 1, null, (byte) 2))
                        .build(),
                SparseListsInput.builder()
                        .listOfDouble(
                                ListUtils.of(2.0, null, 3.0))
                        .build(),
                SparseListsInput.builder()
                        .listOfFloat(
                                ListUtils.of(2f, null, 3f))
                        .build(),
                SparseListsInput.builder()
                        .listOfInteger(
                                ListUtils.of(1, null, 2))
                        .build(),
                SparseListsInput.builder()
                        .listOfLong(
                                ListUtils.of(1L, null, 2L))
                        .build(),
                SparseListsInput.builder()
                        .listOfShort(
                                ListUtils.of((short) 1, null, (short) 2))
                        .build(),
                SparseListsInput.builder()
                        .listOfString(
                                ListUtils.of("a", null, "b"))
                        .build(),
                SparseListsInput.builder()
                        .listOfBlobs(
                                ListUtils.of(
                                        wrap(Base64.getDecoder().decode("YmxvYg==")),
                                        null,
                                        wrap(Base64.getDecoder().decode("YmlyZHM="))))
                        .build(),
                SparseListsInput.builder()
                        .listOfTimestamps(
                                ListUtils.of(Instant.EPOCH, null, Instant.MIN))
                        .build(),
                SparseListsInput.builder()
                        .listOfUnion(
                                ListUtils.of(new NestedUnion.AMember("string"), null, new NestedUnion.BMember(2)))
                        .build(),
                SparseListsInput.builder()
                        .listOfEnum(
                                ListUtils.of(NestedEnum.A, null, NestedEnum.B))
                        .build(),
                SparseListsInput.builder()
                        .listOfIntEnum(
                                ListUtils.of(NestedIntEnum.A, null, NestedIntEnum.B))
                        .build(),
                SparseListsInput.builder()
                        .listOfStruct(
                                ListUtils.of(NestedStruct.builder().build(), null, NestedStruct.builder().build()))
                        .build(),
                SparseListsInput.builder()
                        .listOfDocuments(
                                ListUtils.of(Document.of(2.0), null, Document.of("string")))
                        .build());
    }

    @ParameterizedTest
    @MethodSource({"listTypes", "nestedLists", "sparseLists"})
    void pojoToDocumentRoundTrip(SerializableStruct pojo) {
        var output = Utils.pojoToDocumentRoundTrip(pojo);
        assertEquals(pojo.hashCode(), output.hashCode());
        assertEquals(pojo, output);
    }

    @Test
    void nullDistinctFromEmpty() {
        var emptyInput = ListAllTypesInput.builder().listOfBoolean(List.of()).build();
        var nullInput = ListAllTypesInput.builder().build();
        assertNotEquals(emptyInput, nullInput);
        assertTrue(emptyInput.hasListOfBoolean());
        assertFalse(nullInput.hasListOfBoolean());
        // Collections should return empty collections for access
        assertEquals(emptyInput.getListOfBoolean(), Collections.emptyList());
        assertEquals(emptyInput.getListOfBoolean(), nullInput.getListOfBoolean());
        var emptyDocument = Document.of(emptyInput);
        var nullDocument = Document.of(nullInput);
        assertNotNull(emptyDocument.getMember("listOfBoolean"));
        assertNull(nullDocument.getMember("listOfBoolean"));
    }

    static Stream<String> nonUniqueSources() {
        return Stream.of(
                "{\"setOfBoolean\":[true, false, false]}",
                "{\"setOfNumber\":[1,2,2]}",
                "{\"setOfString\":[\"a\", \"a\", \"b\"]}",
                "{\"setOfBlobs\":[\"YmxvYg==\", \"YmxvYg==\"]}",
                "{\"setOfTimestamps\":[0, 20, 0]}",
                "{\"setOfUnion\":[{\"a\": \"str\"}, {\"b\": 1}, {\"a\": \"str\"}]}",
                "{\"setOfEnum\":[\"A\", \"B\", \"A\"]}",
                "{\"setOfIntEnum\":[1,1,2]}",
                "{\"setOfStruct\":[{\"fieldA\": \"a\"}, {\"fieldA\": \"a\"}, {\"fieldA\": \"z\"}]}",
                "{\"setOfStringList\":[[\"a\", \"b\"],[\"c\", \"d\"],[\"a\", \"b\"]]}",
                "{\"setOfStringMap\": [{\"a\": \"b\", \"c\": \"d\"}, {\"c\": \"d\", \"a\": \"b\"}]}");
    }

    // Uniqueness is enforced in validation only.
    @ParameterizedTest
    @MethodSource("nonUniqueSources")
    void nonUniqueIsAllowed(String source) {
        try (var codec = JsonCodec.builder().useJsonName(true).build()) {
            // This not throwing is the test.
            codec.deserializeShape(source, SetsAllTypesInput.builder());
        }
    }
}
