package kotlinx.benchmarks_build.tasks

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.*

abstract class CheckReadmeTask : DefaultTask() {
    @get:Input
    abstract val minSupportedGradleVersion: Property<String>

    @get:InputFile
    @get:PathSensitive(RELATIVE)
    abstract val readme: RegularFileProperty

    @TaskAction
    fun execute() {
        val readme = readme.get().asFile
        val readmeContents = readme.readText()

        val minSupportedGradleVersion = minSupportedGradleVersion.get()

        val matches = Regex("Gradle (?<version>[^ ]+) or newer").findAll(readmeContents).toList()

        require(matches.size >= 1) {
            """
            $readme does not contain correct min supported Gradle version.
            ${matches.size} matches found.
            """.trimIndent()
        }

        matches.forEach { match ->
            val version = match.groups["version"]?.value ?: error("Regex failed - could not find version")
            require(minSupportedGradleVersion == version) {
                """
                $readme does not contain correct min supported Gradle version
                Actual:   ${match.value}
                Expected: Gradle $minSupportedGradleVersion or newer
                """.trimIndent()
            }
        }
    }
}
