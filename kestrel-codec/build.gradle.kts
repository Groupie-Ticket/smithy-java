plugins {
    id("smithy-java.module-conventions")
}

description = "This module provides kestrel functionality"

extra["displayName"] = "Smithy :: Java :: Kestrel Codec"
extra["moduleName"] = "software.amazon.smithy.java.kestrel"

dependencies {
    api(project(":core"))
    api(project(":kestrel"))
}
