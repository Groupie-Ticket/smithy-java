plugins {
    id("smithy-java.module-conventions")
}

description = "This module provides Server RpcV2 Sparrowhawk functionality"

extra["displayName"] = "Smithy :: Java :: Server Protocols :: RpcV2 Sparrowhawk"
extra["moduleName"] = "software.amazon.smithy.java.server.protocols.rpcV2Sparrowhawk"


dependencies {
    implementation(project(":server"))
    implementation(project(":server-core"))
    implementation(project(":sparrowhawk-codec"))
}