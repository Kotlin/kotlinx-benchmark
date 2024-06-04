package kotlinx.benchmark.integration

import kotlin.test.Test

class MultiplatformTasks : GradleTest() {
    @Test
    fun nativeTasks() {
        project("kotlin-multiplatform-tasks", true).let { runner ->
            val target = "native"
            runner.run(":${target}BenchmarkGenerate")
            val capitalizedTarget = target.replaceFirstChar { it.uppercaseChar() }
            runner.run(":tasks")
            runner.run(":compile${capitalizedTarget}BenchmarkKotlin${capitalizedTarget}")
            runner.run(":run${capitalizedTarget}BenchmarkReleaseExecutable${capitalizedTarget}")
        }
    }
}