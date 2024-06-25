import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.CacheableTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import org.apache.tools.zip.ZipEntry
import org.apache.tools.zip.ZipOutputStream

plugins {
    id("smithy-java.module-conventions")
    alias(libs.plugins.shadow)
}

buildscript {
    configurations {
        val asmVersion = "9.6"
        classpath {
            resolutionStrategy {
                //in order to handle jackson's higher release version in shadow, this needs to be upgraded to latest.
                force( "org.ow2.asm:asm:${asmVersion}")
                force("org.ow2.asm:asm-commons:${asmVersion}")
            }
        }
    }
}

val shadePrefix = "software.amazon.smithy.java.internal"

description = "This module is just used from bundling"

extra["displayName"] = "Smithy :: Java :: VendServer"
extra["moduleName"] = "software.amazon.smithy.java.vend.server"

dependencies {
    implementation(project(":server-netty"))
    implementation(project(":server-protocols:restJson"))
    implementation(project(":server-protocols:rpcV2Kestrel"))
    implementation(project(":core"))
}

val uberJar = tasks.register<ShadowJar>("uberJar") {
    configurations = listOf(project.configurations.runtimeClasspath.get())
    archiveClassifier.set("all")
    relocate("io.netty", "${shadePrefix}.netty")
    relocate("com.jsoniter", "${shadePrefix}.jsoniter")
    relocate("com.fasterxml.jackson", "${shadePrefix}.com.fasterxml.jackson")
    relocate("META-INF/native/libnetty", "META-INF/native/lib${shadePrefix.replace('.', '_')}_netty")
    relocate("META-INF/native/netty", "META-INF/native/${shadePrefix.replace('.', '_')}_netty")
    exclude("META-INF/maven/**")
    transform(NettyResourceTransformer::class.java)
    mergeServiceFiles()
}

tasks.build {
    finalizedBy(uberJar)
}


@CacheableTransformer
class NettyResourceTransformer : Transformer {

    private val resources: MutableMap<String, String> = mutableMapOf()

    override fun getName(): String = "NettyResourceTransformer"

    override fun canTransformResource(fileTreeElement: FileTreeElement): Boolean {
        return fileTreeElement.name.startsWith("META-INF/native-image/io.netty")
    }

    override fun transform(context: TransformerContext)  {
        val updatedPath = context.path.replace("io.netty", "software.amazon.smithy.java.internal.io.netty")
        val updatedContent = context.`is`.bufferedReader().use { it.readText() }.replace("io.netty", "software.amazon.smithy.java.internal.io.netty")
        resources[updatedPath] = updatedContent
    }

    override fun hasTransformedResource(): Boolean {
        return resources.isNotEmpty()
    }


    override fun modifyOutputStream(outputStream: ZipOutputStream, preserveFileTimestamps: Boolean) {
        for ((path, content) in resources) {
            val entry = ZipEntry(path).apply {
                time = TransformerContext.getEntryTimestamp(preserveFileTimestamps, time)
            }
            outputStream.putNextEntry(entry)
            outputStream.write(content.toByteArray())
            outputStream.closeEntry()
        }
    }
}