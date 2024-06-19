$version: "2.0"


namespace smithy.java.codegen.server.test

use aws.auth#sigv4
use aws.protocols#restJson1
use smithy.protocols#idx
use smithy.protocols#indexed
use smithy.protocols#rpcv2Kestrel

@sigv4(name: "restjson")
@restJson1
@rpcv2Kestrel
@indexed
service TestService {
    version: "today"
    operations: [GetBeer, Echo, HashFile, ZipFile, FizzBuzz]
}

@http(method: "POST", uri: "/get-beer")
operation GetBeer {
    input:= {
        @httpHeader("X-Beer-Input-Id")
        @required
        @idx(1)
        id: Long
    }
    output:= {
        @required
        @idx(1)
        value: Beer

        @required
        @httpHeader("X-Beer-Output-Id")
        @idx(2)
        beerId: Long
    }
    errors: [NoSuchBeerException, DependencyException]
}

@http(method: "POST", uri: "/echo")
operation Echo {
    input: EchoInput
    output: EchoOutput
}

@http(method: "POST", uri: "/hash")
operation HashFile {
    input:= {
        @httpPayload
        @required
        @idx(1)
        payload: FileStream
    }

    output:= {
        @idx(1)
        hashcode: String
    }
}

@http(method: "POST", uri: "/gzip")
operation ZipFile {
    input:= {
        @httpPayload
        @required
        @idx(1)
        payload: FileStream
    }

    output:= {
        @httpPayload
        @required
        @idx(1)
        payload: FileStream
    }
}

@http(method: "POST", uri: "/fizzBuzz")
operation FizzBuzz {
    input:= {
        @httpPayload
        @idx(1)
        stream: ValueStream
    }
    output:= {
        @httpPayload
        @idx(1)
        stream: FizzBuzzStream
    }
    errors: [FizzBuzzException]
}

@streaming
union ValueStream {
    @idx(1)
    Value: Value
}

structure Value {
    @idx(1)
    value: Long
}

@streaming
union FizzBuzzStream {
    @idx(1)
    fizz: FizzEvent
    @idx(2)
    buzz: BuzzEvent
    @idx(3)
    negativeNumberException: NegativeNumberException
    @idx(4)
    internalException: InternalException
    @idx(5)
    malformedInputException: MalformedInputException
}

@error("client")
structure NegativeNumberException {
    @idx(1)
    message: String
}

@error("server")
structure InternalException {
    @idx(1)
    message: String
}

@error("client")
structure MalformedInputException {
    @idx(1)
    message: String
}

structure FizzEvent {
    @idx(1)
    value: Long
}

structure BuzzEvent {
    @idx(1)
    value: Long
}

@streaming
blob FileStream

structure EchoInput {
    @idx(1)
    value: EchoPayload
}

structure EchoOutput {
    @idx(1)
    value: EchoPayload
}

structure EchoPayload {
    @idx(1)
    string: String

    @required
    @default(0)
    @idx(2)
    echoCount: Integer
}

structure Beer {
    @idx(1)
    name: String
}

list BeerList {
    member: Beer
}

@error("client")
@httpError(404)
structure NoSuchBeerException {
    @idx(1)
    message: String
}

@error("server")
@httpError(500)
structure DependencyException {
    @idx(1)
    message: String
}

@error("server")
@httpError(500)
structure FizzBuzzException {
    @idx(1)
    message: String
}


