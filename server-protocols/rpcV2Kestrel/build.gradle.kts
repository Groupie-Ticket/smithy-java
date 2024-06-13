plugins {
    id("smithy-java.module-conventions")
}

description = "This module provides Server RpcV2 Kestrel functionality"

extra["displayName"] = "Smithy :: Java :: Server Protocols :: RpcV2 Kestrel"
extra["moduleName"] = "software.amazon.smithy.java.server.protocols.rpcV2Kestrel"


dependencies {
    implementation(project(":server"))
    implementation(project(":server-core"))
    implementation(project(":kestrel-codec"))
}