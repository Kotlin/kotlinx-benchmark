package tasks

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.*

abstract class CheckReadmeTask : DefaultTask() {
    @get:Input
    abstract val minSupportedGradleVersion: Property<String>
    @get:Input
    abstract val minSupportedKotlinVersion: Property<String>

    @get:InputFile
    @get:PathSensitive(RELATIVE)
    abstract val readme: RegularFileProperty

    @TaskAction
    fun execute() {
        val readme = readme.get().asFile
        val readmeContents = readme.readText()

        val minSupportedGradleVersion = minSupportedGradleVersion.get()
        val minSupportedKotlinVersion = minSupportedKotlinVersion.get()

        val matches = Regex("Kotlin (?<kotlinVersion>[^ ]+) or newer and Gradle (?<gradleVersion>[^ ]+) or newer")
            .findAll(readmeContents).toList()

        require(matches.size >= 1) {
            """
            $readme does not contain correct min supported Kotlin and Gradle versions.
            ${matches.size} matches found.
            """.trimIndent()
        }

        matches.forEach { match ->
            val kotlinVersion = match.groups["kotlinVersion"]?.value ?: error("Regex failed - could not find kotlinVersion")
            val gradleVersion = match.groups["gradleVersion"]?.value ?: error("Regex failed - could not find gradleVersion")
            require(minSupportedKotlinVersion == kotlinVersion && minSupportedGradleVersion == gradleVersion) {
                """
                $readme does not contain correct min supported Kotlin and Gradle versions.
                Actual:   ${match.value}
                Expected: Kotlin $minSupportedKotlinVersion or newer and Gradle $minSupportedGradleVersion or newer
                """.trimIndent()
            }
        }
    }
}
