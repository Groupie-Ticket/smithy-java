plugins {
    `java-library`
    application
    id("org.graalvm.buildtools.native") version "0.10.3"
}

dependencies {
    implementation("info.picocli:picocli:4.7.6")
    annotationProcessor("info.picocli:picocli-codegen:4.7.6")

    // Client dependencies
    implementation(project(":aws:client:aws-client-restjson"))
    implementation(project(":aws:client:aws-client-awsjson"))
    implementation(project(":aws:client:aws-client-restxml"))
    implementation(project(":client:client-rpcv2-cbor"))

    implementation(project(":client:dynamic-client"))
    implementation(project(":codecs:json-codec"))
    implementation(project(":client:client-http"))
    implementation(project(":aws:client:aws-client-core"))
    implementation(project(":aws:sigv4"))

    implementation("software.amazon.smithy:smithy-aws-traits:1.56.0")
    implementation("software.amazon.smithy:smithy-protocol-traits:1.56.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

application {
    mainClass = "software.amazon.smithy.java.cli.SmithyCallRunner"
}

graalvmNative {
    binaries.named("main") {
        // Set up correct java JVM to use.
        javaLauncher.set(
            javaToolchains.launcherFor {
                // Use oracle GraalVM JDK for build
                languageVersion.set(JavaLanguageVersion.of(23))
                vendor.set(JvmVendorSpec.matching("Oracle Corporation"))
            },
        )

        // Ensure resources are detected
        resources.autodetect()

        buildArgs.addAll(listOf(
            "-H:ResourceConfigurationFiles=${projectDir}/src/resource-config.json",
            "-H:Log=registerResource:5",
            "-H:+UnlockExperimentalVMOptions",
            "--enable-url-protocols=http,https"
        ))

        // Debug info
        verbose.set(true)

        // Image configuration
        imageName.set("smithy-call")
        mainClass.set(application.mainClass)

        // Determines if image is a shared library [note: defaults to true if java-library plugin is applied]
        sharedLibrary.set(false)
    }

    agent {
        enabled.set(true)
        defaultMode.set("standard")
    }

//    agent {
//        defaultMode.set("standard")
//        enabled.set(true)
//
//        modes {
//            standard {
//            }
//            conditional {
////                userCodeFilterPath.set("path-to-filter.json")
//            }
//            direct {
////                options.add("config-output-dir={./src}")
////                options.add("experimental-configuration-with-origins")
//            }
//        }
//
////        callerFilterFiles.from("filter.json")
////        accessFilterFiles.from("filter.json")
//        builtinCallerFilter.set(true)
//        builtinHeuristicFilter.set(true)
//        enableExperimentalPredefinedClasses.set(false)
//        enableExperimentalUnsafeAllocationTracing.set(false)
//        trackReflectionMetadata.set(true)
//
//        metadataCopy {
//            inputTaskNames.add("test")
//            outputDirectories.add("/META-INF/native-image/<groupId>/<artifactId>/")
//            mergeWithExisting.set(true)
//        }
//    }
}

repositories {
    mavenLocal()
    mavenCentral()
}


