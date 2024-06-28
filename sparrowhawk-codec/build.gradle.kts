plugins {
    id("smithy-java.module-conventions")
}

description = "This module provides sparrowhawk functionality"

extra["displayName"] = "Smithy :: Java :: Sparrowhawk Codec"
extra["moduleName"] = "software.amazon.smithy.java.sparrowhawk"

dependencies {
    api(project(":core"))
    api(project(":sparrowhawk"))
}
