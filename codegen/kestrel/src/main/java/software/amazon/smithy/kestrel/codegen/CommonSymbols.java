/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.kestrel.codegen;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolReference;

public final class CommonSymbols {
    private CommonSymbols() {}

    public enum UseOption implements SymbolReference.Option {
        STATIC
    }

    public static SymbolReference staticImp(String namespace, String name) {
        return Symbol.builder()
            .namespace(namespace, ".")
            .name(name)
            .build()
            .toReference(null, UseOption.STATIC);
    }

    public static SymbolReference imp(String namespace, String name) {
        return Symbol.builder()
            .namespace(namespace, ".")
            .name(name)
            .build()
            .toReference(null);
    }

    public static final SymbolReference UTF_8 = staticImp("java.nio.charset.StandardCharsets", "UTF_8");
    public static final SymbolReference asList = staticImp("java.util.Arrays", "asList");
    public static final SymbolReference toMap = staticImp("java.util.stream.Collectors", "toMap");
    public static final SymbolReference toList = staticImp("java.util.stream.Collectors", "toList");
    public static final SymbolReference Entry = imp("java.util.Map", "Entry");
    public static final SymbolReference SimpleEntry = imp("java.util.AbstractMap", "SimpleEntry");
    public static final SymbolReference Object = imp("java.lang", "Object");
    public static final SymbolReference Objects = imp("java.util", "Objects");
    public static final SymbolReference missingField = staticImp(
        "software.amazon.smithy.java.kestrel.KestrelSerializer",
        "missingField"
    );
    public static final SymbolReference intSize = staticImp(
        "software.amazon.smithy.java.kestrel.KestrelSerializer",
        "intSize"
    );
    public static final SymbolReference longSize = staticImp(
        "software.amazon.smithy.java.kestrel.KestrelSerializer",
        "longSize"
    );
    public static final SymbolReference uintSize = staticImp(
        "software.amazon.smithy.java.kestrel.KestrelSerializer",
        "uintSize"
    );
    public static final SymbolReference ulongSize = staticImp(
        "software.amazon.smithy.java.kestrel.KestrelSerializer",
        "ulongSize"
    );
    public static final SymbolReference byteListLengthEncodedSize = staticImp(
        "software.amazon.smithy.java.kestrel.KestrelSerializer",
        "byteListLengthEncodedSize"
    );
    public static final SymbolReference lenPrefixedListLengthEncodedSize = staticImp(
        "software.amazon.smithy.java.kestrel.KestrelSerializer",
        "lenPrefixedListLengthEncodedSize"
    );
    public static final SymbolReference encodeFourBListLength = staticImp(
        "software.amazon.smithy.java.kestrel.KConstants",
        "encodeFourBListLength"
    );
    public static final SymbolReference encodeEightBListLength = staticImp(
        "software.amazon.smithy.java.kestrel.KConstants",
        "encodeEightBListLength"
    );
    public static final SymbolReference encodeVarintListLength = staticImp(
        "software.amazon.smithy.java.kestrel.KConstants",
        "encodeVarintListLength"
    );
    public static final SymbolReference encodeByteListLength = staticImp(
        "software.amazon.smithy.java.kestrel.KConstants",
        "encodeByteListLength"
    );
    public static final SymbolReference encodeLenPrefixedListLength = staticImp(
        "software.amazon.smithy.java.kestrel.KConstants",
        "encodeLenPrefixedListLength"
    );
    public static final SymbolReference decodeElementCount = staticImp(
        "software.amazon.smithy.java.kestrel.KConstants",
        "decodeElementCount"
    );
    public static final SymbolReference decodeVarintListLengthChecked = staticImp(
        "software.amazon.smithy.java.kestrel.KConstants",
        "decodeVarintListLengthChecked"
    );
    public static final SymbolReference decodeLenPrefixedListLengthChecked = staticImp(
        "software.amazon.smithy.java.kestrel.KConstants",
        "decodeLenPrefixedListLengthChecked"
    );
    public static final SymbolReference decodeFourByteListLengthChecked = staticImp(
        "software.amazon.smithy.java.kestrel.KConstants",
        "decodeFourByteListLengthChecked"
    );
    public static final SymbolReference decodeEightByteListLengthChecked = staticImp(
        "software.amazon.smithy.java.kestrel.KConstants",
        "decodeEightByteListLengthChecked"
    );
    public static final SymbolReference T_LIST = staticImp("software.amazon.smithy.java.kestrel.KConstants", "T_LIST");
    public static final SymbolReference T_VARINT = staticImp(
        "software.amazon.smithy.java.kestrel.KConstants",
        "T_VARINT"
    );
    public static final SymbolReference T_FOUR = staticImp("software.amazon.smithy.java.kestrel.KConstants", "T_FOUR");
    public static final SymbolReference T_EIGHT = staticImp(
        "software.amazon.smithy.java.kestrel.KConstants",
        "T_EIGHT"
    );

    public static final SymbolReference KestrelObject = imp("software.amazon.smithy.java.kestrel", "KestrelObject");
    public static final SymbolReference KestrelStructure = imp(
        "software.amazon.smithy.java.kestrel",
        "KestrelStructure"
    );
    public static final SymbolReference KestrelSerializer = imp(
        "software.amazon.smithy.java.kestrel",
        "KestrelSerializer"
    );
    public static final SymbolReference KestrelDeserializer = imp(
        "software.amazon.smithy.java.kestrel",
        "KestrelDeserializer"
    );

    public static final SymbolReference FloatMap = imp("software.amazon.smithy.java.kestrel", "FloatMap");
    public static final SymbolReference DoubleMap = imp("software.amazon.smithy.java.kestrel", "DoubleMap");
    public static final SymbolReference BooleanMap = imp("software.amazon.smithy.java.kestrel", "BooleanMap");
    public static final SymbolReference ByteMap = imp("software.amazon.smithy.java.kestrel", "ByteMap");
    public static final SymbolReference ShortMap = imp("software.amazon.smithy.java.kestrel", "ShortMap");
    public static final SymbolReference IntegerMap = imp("software.amazon.smithy.java.kestrel", "IntegerMap");
    public static final SymbolReference LongMap = imp("software.amazon.smithy.java.kestrel", "LongMap");
    public static final SymbolReference StringList = imp("software.amazon.smithy.java.kestrel", "StringList");
    public static final SymbolReference SparseStringList = imp(
        "software.amazon.smithy.java.kestrel",
        "SparseStringList"
    );
    public static final SymbolReference StringMap = imp("software.amazon.smithy.java.kestrel", "StringMap");
    public static final SymbolReference StructureMap = imp("software.amazon.smithy.java.kestrel", "StructureMap");
}
