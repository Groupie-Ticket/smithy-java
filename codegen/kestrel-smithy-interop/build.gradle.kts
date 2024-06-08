plugins {
    id("smithy-java.module-conventions")
    id("smithy-java.integ-test-conventions")
}

description = "This module provides the core codegen functionality for Smithy java"
group = "software.amazon.smithy.java.codegen"

extra["displayName"] = "Smithy :: Java :: Codegen :: Core"
extra["moduleName"] = "software.amazon.smithy.java.codegen"

dependencies {
    implementation(libs.smithy.codegen)
    implementation(project(":codegen:core"))
    testImplementation(libs.smithy.aws.traits)
    testImplementation(libs.smithy.protocol.traits)
    implementation(project(":codegen:kestrel"))
    implementation(project(":kestrel-codec"))
    testImplementation(project(":codegen:server"))
}
