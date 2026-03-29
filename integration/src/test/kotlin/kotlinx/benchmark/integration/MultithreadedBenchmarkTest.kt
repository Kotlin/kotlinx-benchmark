package kotlinx.benchmark.integration

import kotlin.test.Test

class MultithreadedBenchmarkTest : GradleTest() {
    private fun verifyFor(target: String) {
        project(
            "multithreaded-benchmarks", true,
            /* we need a fresh Kotlin to use atomics */
            kotlinVersion = KotlinTestVersion.Kotlin2_3_0.versionString,
            gradleVersion = GradleTestVersion.v9_3_0
        ).let { runner ->
            runner.runAndSucceed("${target}Benchmark")
        }
    }

    @Test
    fun native() {
        verifyFor("native")
    }

    @Test
    fun jvm() {
        verifyFor("jvm")
    }

    @Test
    fun js() {
        verifyFor("js")
    }

    @Test
    fun wasmJs() {
        verifyFor("wasmJs")
    }
}
