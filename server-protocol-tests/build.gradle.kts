plugins {
    id("smithy-java.module-conventions")
    id("smithy-java.integ-test-conventions")
}

description = "This module runs server protocol tests"

extra["displayName"] = "Smithy :: Java :: Server Protocol Tests"
extra["moduleName"] = "software.amazon.smithy.java.server.protocoltests"

buildscript {
    dependencies {
    }
}

dependencies {
    implementation(project(":server"))
    implementation(project(":server-core"))
    implementation(project(":http-binding"))
    implementation(project(":json-codec"))
    testImplementation(project(":codegen::server"))
    testImplementation(project(":server-netty"))
    testImplementation(project(":server-protocols::restJson"))
    testImplementation(libs.smithy.codegen)
    testImplementation(libs.smithy.aws.protocol.tests)
}


val generatedSrcDir = layout.buildDirectory.dir("generated-src").get()
val generateSrcTask = tasks.register<JavaExec>("generateSources") {
    delete(files(generatedSrcDir))
    dependsOn("test")
    classpath = sourceSets["test"].runtimeClasspath + sourceSets["test"].output + sourceSets["it"].resources.sourceDirectories
    mainClass = "software.amazon.smithy.java.server.protocoltests.ProtocolTestCodegenRunner"
    environment("output", generatedSrcDir)
}

// Add generated POJOs to integ tests and jmh benchmark
sourceSets {
    it {
        java {
            srcDir(generatedSrcDir)
        }
        resources {
            srcDir(generatedSrcDir)
        }
    }
}

tasks {
    integ {
        dependsOn(generateSrcTask)
    }
    compileItJava {
        dependsOn(generateSrcTask)
    }
    spotbugsIt {
        enabled = false
    }
}

// Ignore generated generated code for formatter check
spotless {
    java {
        targetExclude("**/build/**/*.*")
    }
}