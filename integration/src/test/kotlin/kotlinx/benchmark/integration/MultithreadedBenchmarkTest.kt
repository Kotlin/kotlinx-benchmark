package kotlinx.benchmark.integration

import kotlin.io.path.readText
import kotlin.test.assertContains
import kotlin.test.Test

class MultithreadedBenchmarkTest : GradleTest() {
    private fun verifyFor(target: String, expectedThreads: Int, warning: String? = null) {
        val runner = project(
            "multithreaded-benchmarks", true,
            /* we need a fresh Kotlin to use atomics */
            kotlinVersion = KotlinTestVersion.Kotlin2_3_0.versionString,
            gradleVersion = GradleTestVersion.v9_3_0
        )

        runner.runAndSucceed("${target}Benchmark") {
            warning?.let(::assertOutputContains)
            if (warning == null) {
                assertOutputDoesNotContain("runtime is single-threaded")
            }
        }

        val report = reports("main").single { it.name == "$target.json" }.toPath().readText()
        assertContains(report, """"threads" : $expectedThreads""")
    }

    @Test
    fun native() {
        verifyFor("native", expectedThreads = 2)
    }

    @Test
    fun jvm() {
        verifyFor("jvm", expectedThreads = 2)
    }

    @Test
    fun js() {
        verifyFor(
            "js",
            expectedThreads = 1,
            warning = "WARNING: Runtime is single-threaded, so requested benchmark threads value is ignored and only one thread will be used."
        )
    }

    @Test
    fun wasmJs() {
        verifyFor(
            "wasmJs",
            expectedThreads = 1,
            warning = "WARNING: Runtime is single-threaded, so requested benchmark threads value is ignored and only one thread will be used."
        )
    }
}
