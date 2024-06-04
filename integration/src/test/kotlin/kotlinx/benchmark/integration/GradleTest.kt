package kotlinx.benchmark.integration

import org.junit.*
import org.junit.rules.*
import java.io.*

abstract class GradleTest {
    private class NonDeletableTemporaryFolder(file: File) : TemporaryFolder(file) {
        override fun after() { }
    }

    @Rule
    @JvmField
    internal val testProjectDir: TemporaryFolder = NonDeletableTemporaryFolder(File("build/temp").apply { mkdirs() })
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
        build: ProjectBuilder.() -> Unit = {}
    ): Runner {
        templates.resolve(name).copyRecursively(rootProjectDir)
        val buildFile = file("build.gradle.kts")
            .takeIf { it.exists() }
            ?: file("build.gradle")

        val isKts = buildFile.extension == "kts"
        val builder = ProjectBuilder(isKts).apply {
            kotlinVersion?.let { this.kotlinVersion = it }
            build()
        }
        buildFile.modify(builder::build)
        if (isKts) {
            val settingsFile = file("settings.gradle.kts")
            if (!settingsFile.exists()) {
                file("settings.gradle.kts").writeText("""
                    pluginManagement {
                        repositories {
                            maven("${System.getProperty("plugin_repo_url")}")
                            gradlePluginPortal()
                        }
                    }
                    """.trimIndent())
            }
        } else {
            val settingsFile = file("settings.gradle")
            if (!settingsFile.exists()) {
                file("settings.gradle").writeText("") // empty settings file
            }
        }
        return Runner(rootProjectDir, print, gradleVersion)
    }
}

private val templates = File("src/test/resources/templates")

private fun File.modify(fn: (String) -> String) {
    writeText(fn(readText()))
}
