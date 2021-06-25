package kotlinx.benchmark.integration

import org.gradle.testkit.runner.*
import java.io.*

class Runner(
    private val projectDir: File,
    private val print: Boolean
) {

    private fun gradle(vararg tasks: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(*(defaultArguments() + tasks))
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

}
