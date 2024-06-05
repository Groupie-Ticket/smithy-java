$version: "2"

namespace smithy.java.codegen.test.structures

use smithy.java.codegen.test.structures.members#EventStreaming

resource StructureTests {
    operations: [
        BlobMembers
        BooleanMembers
        DocumentMembers
        ListMembers
        SetMembers
        MapMembers
        BigDecimalMembers
        BigIntegerMembers
        ByteMembers
        DoubleMembers
        FloatMembers
        IntegerMembers
        LongMembers
        ShortMembers
        StringMembers
        StructureMembers
        TimestampMembers
        UnionMembers
        EnumMembers
        IntEnumMembers
        ClientErrorCorrection
        Defaults
        EventStreaming
    ]
}
