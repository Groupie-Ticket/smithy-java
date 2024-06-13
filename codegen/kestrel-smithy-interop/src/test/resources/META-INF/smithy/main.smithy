$version: "2.0"


namespace smithy.java.codegen.kestrel.test

use aws.auth#sigv4
use aws.protocols#restJson1
use smithy.protocols#idx
use smithy.protocols#indexed

@sigv4(name: "restjson")
@restJson1
@indexed
service TestService {
    version: "today"
    operations: [GetBeer, Echo]
}

@http(method: "POST", uri: "/get-beer")
operation GetBeer {
    input: GetBeerInput
    output: GetBeerOutput
}

@http(method: "POST", uri: "/echo")
operation Echo {
    input: EchoInput
    output: EchoOutput
}

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
    @idx(3)
    booleanMember: Boolean
}

structure Beer {
    @idx(1)
    name: String
    @idx(2)
    id: Long
}

list BeerList {
    member: Beer
}

structure GetBeerInput {
    @required
    @idx(1)
    id: Long
}

structure GetBeerOutput {
    @required
    @idx(1)
    value: BeerList
}


