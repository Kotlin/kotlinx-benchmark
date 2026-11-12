package kotlinx.benchmark.integration

import org.junit.Test

class KotlinNativeTest : GradleTest() {
    @Test
    fun debugBenchmarkTest() {
        project("kotlin-native").let { runner ->
            val target = "native"
            val capitalizedTarget = target.replaceFirstChar { it.uppercaseChar() }

            runner.runAndSucceed(":${target}BenchmarkGenerate")
            runner.runAndSucceed(":compile${capitalizedTarget}BenchmarkKotlin${capitalizedTarget}")
            runner.runAndSucceed(":${capitalizedTarget}Benchmark")
        }
    }

    @Test
    fun perIterationForkMode() {
        project("per-iteration-fork-mode").let { runner ->
            runner.runAndSucceed(":nativePerIterationBenchmark") {
                assertOutputContains("test.CommonBenchmark.mathBenchmark")
                repeat(2) {
                    assertOutputContains("Warm-up #${it}")
                }
                repeat(3) {
                    assertOutputContains("Iteration #${it}")
                }
            }
        }
    }

    @Test
    fun perIterationForkModeWithoutWarmup() {
        project("per-iteration-fork-mode").let { runner ->
            runner.runAndSucceed(":nativeNoWarmupBenchmark") {
                assertOutputContains("test.CommonBenchmark.mathBenchmark")
                assertOutputDoesNotContain("Warm-up #")
                repeat(3) {
                    assertOutputContains("Iteration #${it}")
                }
            }
        }
    }
}
