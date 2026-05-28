package kotlinx.benchmark.integration

import kotlin.io.path.readText
import kotlin.test.assertContains
import kotlin.test.Test

class MultithreadedBenchmarkTest : GradleTest() {
    private fun verifyFor(
        target: String,
        expectedThreads: Int,
        warning: String? = null,
        suppress: Boolean = false,
        threadsInConfig: Int? = null
    ) {
        val runner = project(
            "multithreaded-benchmarks", true,
            /* we need a fresh Kotlin to use atomics */
            kotlinVersion = KotlinTestVersion.Kotlin2_3_0.versionString,
            gradleVersion = GradleTestVersion.v9_3_0
        ) {
            threadsInConfig?.let {
                configuration("main") {
                    threads = it
                }
            }
        }

        val args = buildList {
            add("${target}Benchmark")
            if (suppress) add("-Pbenchmarks_suppress_threads_warning=true")
        }

        runner.runAndSucceed(*args.toTypedArray()) {
            warning?.let(::assertOutputContains)
            if (warning == null) {
                assertOutputDoesNotContain("benchmarks_suppress_threads_warning")
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
            warning = "Using @Threads annotation with value other than 1 will have no effect on the JS platform. The value: 2. To suppress this warning, set Gradle property benchmarks_suppress_threads_warning=true"
        )
    }

    @Test
    fun jsWithConfigThreads() {
        verifyFor(
            "js",
            expectedThreads = 1,
            threadsInConfig = 3,
            warning = "Using threads configuration parameter with value other than 1 will have no effect for the target wasmJs. The value: 3. To suppress this warning, set Gradle property benchmarks_suppress_threads_warning=true"
        )
    }

    @Test
    fun jsWithSuppression() {
        verifyFor(
            "js",
            expectedThreads = 1,
            threadsInConfig = 100500,
            suppress = true
        )
    }

    @Test
    fun wasmJs() {
        verifyFor(
            "wasmJs",
            expectedThreads = 1,
            warning = "Using @Threads annotation with value other than 1 will have no effect on the Wasm platform. The value: 2. To suppress this warning, set Gradle property benchmarks_suppress_threads_warning=true"
        )
    }

    @Test
    fun wasmJsThreadsConfig() {
        verifyFor(
            "wasmJs",
            expectedThreads = 1,
            threadsInConfig = 3,
            warning = "Using @Threads annotation with value other than 1 will have no effect on the Wasm platform. The value: 2. To suppress this warning, set Gradle property benchmarks_suppress_threads_warning=true"
        )
    }

    @Test
    fun wasmJsWithSuppression() {
        verifyFor(
            "wasmJs",
            expectedThreads = 1,
            threadsInConfig = 100500,
            suppress = true
        )
    }
}
