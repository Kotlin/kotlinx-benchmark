package kotlinx.benchmark.integration

import kotlinx.benchmark.integration.GradleTestVersion.MinSupportedGradleVersion
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

class Runner(
    private val projectDir: File,
    private val print: Boolean,
    gradleVersion: GradleTestVersion? = null,
) {
    /** Defaults to the minimum Gradle version specified in [kotlinx.benchmark.gradle.BenchmarksPlugin] */
    private val gradleVersion: GradleTestVersion = gradleVersion ?: MinSupportedGradleVersion

    private fun gradle(vararg tasks: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(*(defaultArguments() + tasks))
            .withGradleVersion(gradleVersion.versionString)
            .run {
                if (print) forwardStdOutput(System.out.bufferedWriter()) else this
            }

    fun run(vararg tasks: String, fn: BuildResult.() -> Unit = {}) {
        val gradle = gradle(*tasks)
        val buildResult = gradle.build()
        buildResult.fn()
    }

    fun runAndFail(vararg tasks: String, fn: BuildResult.() -> Unit = {}) {
        val gradle = gradle(*tasks)
        val buildResult = gradle.buildAndFail()
        buildResult.fn()
    }

    private fun defaultArguments(): Array<String> = arrayOf("--stacktrace")

    fun updateAnnotations(filePath: String, annotationsSpecifier: AnnotationsSpecifier.() -> Unit) {
        val annotations = AnnotationsSpecifier().also(annotationsSpecifier)
        val file = projectDir.resolve(filePath)

        val updatedLines = file.readLines().map {
            annotations.replaceClassAnnotation(it)
        }
        annotations.checkAllAnnotationsAreUsed()

        file.writeText(updatedLines.joinToString(separator = "\n"))
        if (print) {
            println(file.readText())
        }
    }

    fun addAnnotation(filePath: String, annotationsSpecifier: AnnotationsSpecifier.() -> Unit) {
        val annotations = AnnotationsSpecifier().also(annotationsSpecifier)
        val file = projectDir.resolve(filePath)

        val updatedLines = mutableListOf<String>()

        file.readLines().forEach { line ->
            val indentation = " ".repeat(line.length - line.trimStart().length)
            annotations.annotationsForFunction(line).forEach { annotation ->
                updatedLines.add(indentation + annotation)
            }
            annotations.annotationsForProperty(line).forEach { annotation ->
                updatedLines.add(indentation + annotation)
            }
            updatedLines.add(line)
        }
        annotations.checkAllAnnotationsAreUsed()

        file.writeText(updatedLines.joinToString(separator = "\n"))
        if (print) {
            println(file.readText())
        }
    }

    fun generatedDir(targetName: String, filePath: String, fileTestAction: (File) -> Unit) {
        fileTestAction(
            projectDir.resolve("build/benchmarks/${targetName}/sources/kotlinx/benchmark/generated").resolve(filePath)
        )
    }
}
