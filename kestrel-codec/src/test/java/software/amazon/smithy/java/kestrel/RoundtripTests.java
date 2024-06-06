/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.kestrel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class RoundtripTests {
    @MethodSource("maps")
    @ParameterizedTest(name = "{0}")
    public void smallMaps(String name, Object n, Supplier supp) {
        runTest(generateMap(n, 1), supp);
    }

    @MethodSource("maps")
    @ParameterizedTest(name = "{0}")
    public void bigMaps(String name, Object n, Supplier supp) {
        runTest(generateMap(n, 10000), supp);
    }

    public static Stream<Arguments> maps() {
        return Stream.of(
            Arguments.of("BooleanMap", false, (Supplier) BooleanMap::new),
            Arguments.of("ByteMap", (byte) 4, (Supplier) ByteMap::new),
            Arguments.of("ShortMap", (short) 4, (Supplier) ShortMap::new),
            Arguments.of("IntegerMap", 123, (Supplier) IntegerMap::new),
            Arguments.of("LongMap", 9199192222L, (Supplier) LongMap::new),
            Arguments.of("FloatMap", 3.14f, (Supplier) FloatMap::new),
            Arguments.of("DoubleMap", 3.14d, (Supplier) DoubleMap::new)
        );
    }

    @Test
    public void ints() {
        byte[] payload = new byte[100];
        KestrelSerializer s = new KestrelSerializer(payload);
        List<Integer> ints = Arrays.asList(0, 1, -1, 2, -2, Integer.MAX_VALUE, Integer.MIN_VALUE);
        ints.forEach(s::writeVarI);

        KestrelDeserializer d = new KestrelDeserializer(payload);
        for (int i : ints) {
            assertEquals(i, d.varI());
        }
    }

    @Test
    public void longs() {
        byte[] payload = new byte[100];
        KestrelSerializer s = new KestrelSerializer(payload);
        List<Long> longs = Arrays.asList(0L, 1L, -1L, 2L, -2L, Long.MIN_VALUE, Long.MAX_VALUE);
        longs.forEach(s::writeVarL);

        KestrelDeserializer d = new KestrelDeserializer(payload);
        for (long l : longs) {
            assertEquals(l, d.varL());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10000})
    public void stringMap(int len) {
        Map<String, String> m = generateMap("hello", len);
        StringMap sm = new StringMap();
        sm.fromMap(m);
        StringMap roundtrip = serde(sm, new StringMap());
        assertEquals(m, roundtrip.toMap());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10000})
    public void structureMap(int len) {
        Map<String, KestrelCodegenOptionalStruct> map = generateMap(RoundtripTests::makeStruct, len);
        StructureMap<KestrelCodegenOptionalStruct> m = new StructureMap<>(KestrelCodegenOptionalStruct::new);
        m.fromMap(map);
        StructureMap<KestrelCodegenOptionalStruct> roundtrip = serde(
            m,
            new StructureMap<>(KestrelCodegenOptionalStruct::new)
        );
        assertEquals(map, roundtrip.toMap());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10000})
    public void stringList(int len) {
        List<String> list = generateList(Integer::toString, len);
        StringList roundtrip = serde(StringList.fromList(list), new StringList());
        assertEquals(list, roundtrip.toList());
    }

    private static KestrelCodegenOptionalStruct makeStruct() {
        KestrelCodegenOptionalStruct struct = new KestrelCodegenOptionalStruct();
        struct.setString("hello");
        struct.setTimestamp(123.456d);
        return struct;
    }

    private static <T> void runTest(Map<String, T> map, Supplier<NumberMap<T>> supp) {
        NumberMap<T> m = supp.get();
        m.fromMap(map);
        NumberMap<T> roundtrip = serde(m, supp.get());
        assertEquals(map, roundtrip.toMap());
    }

    private static <T extends KestrelObject> T serde(T obj, T base) {
        byte[] ser = ser(obj);
        return de(base, ser);
    }

    private static byte[] ser(KestrelObject o) {
        KestrelSerializer s = new KestrelSerializer(o.size());
        o.encodeTo(s);
        return s.payload();
    }

    private static <T extends KestrelObject> T de(T base, byte[] payload) {
        KestrelDeserializer d = new KestrelDeserializer(payload);
        base.decodeFrom(d);
        d.done();
        return base;
    }

    private static <T> Map<String, T> generateMap(T val, int count) {
        return generateMap(() -> val, count);
    }

    private static <T> Map<String, T> generateMap(Supplier<T> val, int count) {
        Map<String, T> map = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            map.put(Integer.toString(i), val.get());
        }
        return map;
    }

    private static <T> List<T> generateList(IntFunction<T> generator, int count) {
        List<T> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(generator.apply(i));
        }
        return list;
    }
}
