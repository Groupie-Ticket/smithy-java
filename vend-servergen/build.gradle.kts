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


description = "This module is just used from bundling"

extra["displayName"] = "Smithy :: Java :: VendServerGen"
extra["moduleName"] = "software.amazon.smithy.java.vend.servergen"

dependencies {
    implementation(project(":codegen::server"))
    implementation(project(":server-core"))
    implementation(project(":codegen::kestrel-smithy-interop"))
    implementation(project(":codegen::kestrel"))
    implementation(project(":kestrel"))
    implementation(project(":kestrel-codec"))
    implementation(project(":internal-traits"))
    implementation(libs.smithy.aws.traits)
    implementation(libs.smithy.protocol.traits)
    implementation(libs.smithy.codegen)
    implementation(libs.smithy.model)

}

val uberJar = tasks.register<ShadowJar>("uberJar") {
    from(sourceSets["main"].output)
    archiveClassifier.set("all")
    configurations = listOf(project.configurations.runtimeClasspath.get())
    mergeServiceFiles()
    transform(SmithyManifestTransformer::class.java)
    manifest {
        attributes(mapOf("Main-Class" to "software.amazon.smithy.java.server.codegen.CodegenRunner"))
    }
}

tasks.build {
    finalizedBy(uberJar)
}


@CacheableTransformer
class SmithyManifestTransformer : Transformer {
    private val smithyManifestPath = "META-INF/smithy/manifest"

    private val merged = mutableListOf<String>()

    override fun getName(): String = "SmithyManifestTransformer"

    override fun canTransformResource(element: FileTreeElement): Boolean {
        return element.relativePath.pathString == smithyManifestPath
    }

    override fun transform(context: TransformerContext) {
        val lines = context.`is`.bufferedReader().lines().toList()
        merged.addAll(lines)
    }

    override fun hasTransformedResource(): Boolean {
        return merged.isNotEmpty()
    }

    override fun modifyOutputStream(os: ZipOutputStream, preserveFileTimestamps: Boolean) {
        os.putNextEntry(ZipEntry(smithyManifestPath))
        os.write((merged.joinToString("\n") + "\n").toByteArray())
        os.closeEntry()
    }
}