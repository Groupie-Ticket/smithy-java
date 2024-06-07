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
    public static final SymbolReference missingField = staticImp("kestrel.KestrelSerializer", "missingField");
    public static final SymbolReference intSize = staticImp("kestrel.KestrelSerializer", "intSize");
    public static final SymbolReference longSize = staticImp("kestrel.KestrelSerializer", "longSize");
    public static final SymbolReference uintSize = staticImp("kestrel.KestrelSerializer", "uintSize");
    public static final SymbolReference ulongSize = staticImp("kestrel.KestrelSerializer", "ulongSize");
    public static final SymbolReference byteListLengthEncodedSize = staticImp(
        "kestrel.KestrelSerializer",
        "byteListLengthEncodedSize"
    );
    public static final SymbolReference lenPrefixedListLengthEncodedSize = staticImp(
        "kestrel.KestrelSerializer",
        "lenPrefixedListLengthEncodedSize"
    );
    public static final SymbolReference encodeFourBListLength = staticImp(
        "kestrel.KConstants",
        "encodeFourBListLength"
    );
    public static final SymbolReference encodeEightBListLength = staticImp(
        "kestrel.KConstants",
        "encodeEightBListLength"
    );
    public static final SymbolReference encodeVarintListLength = staticImp(
        "kestrel.KConstants",
        "encodeVarintListLength"
    );
    public static final SymbolReference encodeByteListLength = staticImp("kestrel.KConstants", "encodeByteListLength");
    public static final SymbolReference encodeLenPrefixedListLength = staticImp(
        "kestrel.KConstants",
        "encodeLenPrefixedListLength"
    );
    public static final SymbolReference decodeElementCount = staticImp("kestrel.KConstants", "decodeElementCount");
    public static final SymbolReference decodeVarintListLengthChecked = staticImp(
        "kestrel.KConstants",
        "decodeVarintListLengthChecked"
    );
    public static final SymbolReference decodeLenPrefixedListLengthChecked = staticImp(
        "kestrel.KConstants",
        "decodeLenPrefixedListLengthChecked"
    );
    public static final SymbolReference decodeFourByteListLengthChecked = staticImp(
        "kestrel.KConstants",
        "decodeFourByteListLengthChecked"
    );
    public static final SymbolReference decodeEightByteListLengthChecked = staticImp(
        "kestrel.KConstants",
        "decodeEightByteListLengthChecked"
    );
    public static final SymbolReference T_LIST = staticImp("kestrel.KConstants", "T_LIST");
    public static final SymbolReference T_VARINT = staticImp("kestrel.KConstants", "T_VARINT");
    public static final SymbolReference T_FOUR = staticImp("kestrel.KConstants", "T_FOUR");
    public static final SymbolReference T_EIGHT = staticImp("kestrel.KConstants", "T_EIGHT");

    public static final SymbolReference KestrelObject = imp("kestrel", "KestrelObject");
    public static final SymbolReference KestrelStructure = imp("kestrel", "KestrelStructure");
    public static final SymbolReference KestrelSerializer = imp("kestrel", "KestrelSerializer");
    public static final SymbolReference KestrelDeserializer = imp("kestrel", "KestrelDeserializer");

    public static final SymbolReference FloatMap = imp("kestrel", "FloatMap");
    public static final SymbolReference DoubleMap = imp("kestrel", "DoubleMap");
    public static final SymbolReference BooleanMap = imp("kestrel", "BooleanMap");
    public static final SymbolReference ByteMap = imp("kestrel", "ByteMap");
    public static final SymbolReference ShortMap = imp("kestrel", "ShortMap");
    public static final SymbolReference IntegerMap = imp("kestrel", "IntegerMap");
    public static final SymbolReference LongMap = imp("kestrel", "LongMap");
    public static final SymbolReference StringList = imp("kestrel", "StringList");
    public static final SymbolReference SparseStringList = imp("kestrel", "SparseStringList");
    public static final SymbolReference StringMap = imp("kestrel", "StringMap");
    public static final SymbolReference StructureMap = imp("kestrel", "StructureMap");
}
