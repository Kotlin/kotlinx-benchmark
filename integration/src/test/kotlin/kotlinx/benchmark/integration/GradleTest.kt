package kotlinx.benchmark.integration

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

abstract class GradleTest {
    @Rule
    @JvmField
    internal val testProjectDir: TemporaryFolder = TemporaryFolder(File("build/temp").apply { mkdirs() })
    private val rootProjectDir: File get() = testProjectDir.root

    fun file(path: String): File = rootProjectDir.resolve(path)

    fun reports(configuration: String): List<File> {
        val folder = file("build/reports/benchmarks/$configuration")
        return folder.listFiles().orEmpty().flatMap { it?.listFiles().orEmpty().toList() }
    }

    fun project(
        name: String,
        print: Boolean = false,
        gradleVersion: GradleTestVersion? = null,
        kotlinVersion: String? = null,
        jvmToolchain: Int? = null,
        build: ProjectBuilder.() -> Unit = {}
    ): Runner {
        val builder = ProjectBuilder().apply {
            kotlinVersion?.let { this.kotlinVersion = it }
            jvmToolchain?.let { this.jvmToolchain = it }
        }.apply(build)
        rootProjectDir.deleteRecursively()
        templates.resolve(name).copyRecursively(rootProjectDir)
        file("build.gradle").modify(builder::generateBuildScript)
        val settingsFile = file("settings.gradle")
        if (settingsFile.exists()) {
            settingsFile.modify(builder::generateSettingsScripts)
        } else {
            settingsFile.writeText(builder.generateSettingsScripts(""))
        }
        return Runner(
            rootProjectDir, print, gradleVersion,
            // If a Kotlin version was explicitly specified, use it as a native version,
            // otherwise - take a value from the system property.
            kotlinVersion ?: System.getProperty("kotlin.native.version")
        )
    }
}

private val templates = File("src/test/resources/templates")

private fun File.modify(fn: (String) -> String) {
    writeText(fn(readText()))
}
