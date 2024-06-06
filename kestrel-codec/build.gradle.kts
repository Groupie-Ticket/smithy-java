plugins {
    id("smithy-java.module-conventions")
}

description = "This module provides kestrel functionality"

extra["displayName"] = "Smithy :: Java :: Kestrel"
extra["moduleName"] = "software.amazon.smithy.java.kestrel"

dependencies {
    api(project(":core"))
}
