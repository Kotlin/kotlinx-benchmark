package kotlinx.benchmark.integration

import org.junit.*
import org.junit.rules.*
import java.io.*

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
        build: ProjectBuilder.() -> Unit = {}
    ): Runner {
        val builder = ProjectBuilder().apply {
            kotlinVersion?.let { this.kotlinVersion = it }
        }.apply(build)
        templates.resolve(name).copyRecursively(rootProjectDir)
        file("build.gradle").modify(builder::build)
        val settingsFile = file("settings.gradle")
        if (!settingsFile.exists()) {
            file("settings.gradle").writeText("") // empty settings file
        }
        return Runner(rootProjectDir, print, gradleVersion)
    }
}

private val templates = File("src/test/resources/templates")

private fun File.modify(fn: (String) -> String) {
    writeText(fn(readText()))
}
