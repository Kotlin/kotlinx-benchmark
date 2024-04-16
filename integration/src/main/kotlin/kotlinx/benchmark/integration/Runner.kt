package kotlinx.benchmark.integration

import kotlinx.benchmark.integration.GradleTestVersion.MinSupportedGradleVersion
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

class Runner(
    private val projectDir: File,
    private val print: Boolean,
    private val gradleVersion: GradleTestVersion? = null,
) {
    private fun gradle(vararg tasks: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(*(defaultArguments() + tasks))
            .withGradleVersion((gradleVersion ?: MinSupportedGradleVersion).versionString)
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
        val updatedLines = file.readLines().map { annotations.replacementForLine(it) }
        file.writeText(updatedLines.joinToString(separator = "\n"))
    }

    fun generatedDir(targetName: String, filePath: String, fileTestAction: (File) -> Unit) {
        fileTestAction(
            projectDir.resolve("build/benchmarks/${targetName}/sources/kotlinx/benchmark/generated").resolve(filePath)
        )
    }
}
