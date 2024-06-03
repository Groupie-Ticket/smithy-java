plugins {
    id("smithy-java.module-conventions")
}

description = "This module provides the server functionality"

extra["displayName"] = "Smithy :: Java :: Server"
extra["moduleName"] = "software.amazon.smithy.java.server"

dependencies {
    api(project(":core"))
}
