$version: "2.0"


namespace smithy.java.codegen.server.test

use aws.auth#sigv4
use aws.protocols#restJson1

@sigv4(name: "restjson")
@restJson1
service TestService {
    version: "today"
    operations: [GetBeer, Echo]
}

operation GetBeer {
    input: GetBeerInput
    output: GetBeerOutput
}

operation Echo {
    input: EchoInput
    output: EchoOutput
}

structure EchoInput {
    value: EchoPayload
}

structure EchoOutput {
    value: EchoPayload
}

structure EchoPayload {
    string: String
    @required
    @default(0)
    echoCount: Integer
}

structure Beer {
    name: String
    id: Long
}

list BeerList {
    member: Beer
}

structure GetBeerInput {
    @required
    id: Long
}

structure GetBeerOutput {
    @required
    value: BeerList
}


