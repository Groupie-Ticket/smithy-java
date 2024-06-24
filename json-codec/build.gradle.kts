plugins {
    id("smithy-java.module-conventions")
}

description = "This module provides json functionality"

extra["displayName"] = "Smithy :: Java :: JSON"
extra["moduleName"] = "software.amazon.smithy.java.json"

dependencies {
    api(project(":core"))
    api(libs.jsoniter)
    implementation(libs.jackson.core)
    implementation(libs.jackson.databind) // for documents :(
}

tasks.withType<Test> {
    useJUnitPlatform()
    options {
        systemProperty("smithy-java.use-jackson", "true")
    }
}
