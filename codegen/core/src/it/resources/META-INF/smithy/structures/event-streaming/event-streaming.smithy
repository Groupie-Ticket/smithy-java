$version: "2"

namespace smithy.java.codegen.test.structures.members

operation EventStreaming {
    input := {
        @required
        inputStream: StreamType
    }

    output := {
        @required
        outputStream: StreamType
    }
}

@private
@streaming
union StreamType {
    value: StreamValue
    error: StreamError
}

@private
structure StreamValue {
    value: Integer
}

@private
@error("client")
structure StreamError {
    @required
    message: String
}

