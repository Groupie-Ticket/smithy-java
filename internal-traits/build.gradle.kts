plugins {
    id("smithy-java.module-conventions")
}

description = "This module provides traits that are currently private to this project"

extra["displayName"] = "Smithy :: Java :: Internal Traits"
extra["moduleName"] = "software.amazon.smithy.java.internal.traits"

dependencies {
    api(libs.smithy.model)
    api(project(":core"))
}
