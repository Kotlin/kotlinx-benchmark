package kotlinx.benchmark.integration

import org.junit.Test

class KotlinNativeTest : GradleTest() {
    @Test
    fun debugBenchmarkTest() {
        project("kotlin-native", true).let { runner ->
            val target = "native"
            val capitalizedTarget = target.replaceFirstChar { it.uppercaseChar() }

            runner.runAndSucceed(":${target}BenchmarkGenerate")
            runner.runAndSucceed(":compile${capitalizedTarget}BenchmarkKotlin${capitalizedTarget}")
            runner.runAndSucceed(":${capitalizedTarget}Benchmark")
        }
    }
}