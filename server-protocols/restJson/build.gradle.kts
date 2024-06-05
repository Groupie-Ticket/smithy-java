plugins {
    id("smithy-java.module-conventions")
}

description = "This module provides Server RestJson functionality"

extra["displayName"] = "Smithy :: Java :: Server Protocols :: RestJson"
extra["moduleName"] = "software.amazon.smithy.java.server.protocols.restjson"


dependencies {
    implementation(project(":server"))
    implementation(project(":server-core"))
    implementation(project(":http-binding"))
    implementation(project(":json-codec"))
}