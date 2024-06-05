plugins {
    id("smithy-java.module-conventions")
    id("smithy-java.integ-test-conventions")
}

description = "This module provides the server functionality"

extra["displayName"] = "Smithy :: Java :: Server"
extra["moduleName"] = "software.amazon.smithy.java.server"

dependencies {
    implementation(libs.netty.all)
    implementation(project(":server"))
    implementation(project(":server-core"))
    implementation(project(":core"))
    itImplementation(libs.netty.all)
    itImplementation(project(":server"))
    itImplementation(project(":server-core"))
    itImplementation(project(":core"))
    testImplementation(project(":codegen::server"))
    implementation(libs.smithy.codegen)
}


val generatedSrcDir = layout.buildDirectory.dir("generated-src").get()
val generateSrcTask = tasks.register<JavaExec>("generateSources") {
    delete(files(generatedSrcDir))
    dependsOn("test")
    classpath = sourceSets["test"].runtimeClasspath + sourceSets["test"].output + sourceSets["it"].resources.getSourceDirectories()
    mainClass = "software.amazon.smithy.java.server.netty.TestServerJavaCodegenRunner"
    environment("service", "smithy.java.codegen.server.test#TestService")
    environment("namespace", "smithy.java.codegen.server.test")
    environment("output", generatedSrcDir)
}

// Add generated POJOs to integ tests and jmh benchmark
sourceSets {
    it {
        java {
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