plugins {
    id("smithy-java.module-conventions")
    id("smithy-java.integ-test-conventions")
}

description = "This module provides the codegen functionality for Smithy Kestrel"
group = "software.amazon.smithy.java.codegen"

extra["displayName"] = "Smithy :: Java :: Codegen :: Kestrel"
extra["moduleName"] = "software.amazon.smithy.java.codegen"

dependencies {
    implementation(libs.smithy.codegen)
    implementation(project(":internal-traits"))
    implementation(project(":core"))
    implementation(project(":kestrel-codec"))
}