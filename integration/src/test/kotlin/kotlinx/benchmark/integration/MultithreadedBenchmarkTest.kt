package kotlinx.benchmark.integration

import kotlin.io.path.readText
import kotlin.test.assertContains
import kotlin.test.Test

class MultithreadedBenchmarkTest : GradleTest() {
    private fun verifyFor(
        target: String,
        expectedThreads: Int,
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
        }

        runner.runAndSucceed(*args.toTypedArray())

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
        verifyFor("js", expectedThreads = 1 /* threads are not supported */)
    }

    @Test
    fun jsWithConfigThreads() {
        verifyFor(
            "js",
            expectedThreads = 1 /* threads are not supported */,
            threadsInConfig = 3,
        )
    }

    @Test
    fun wasmJs() {
        verifyFor("wasmJs", expectedThreads = 1 /* threads are not supported */)
    }

    @Test
    fun wasmJsThreadsConfig() {
        verifyFor(
            "wasmJs",
            expectedThreads = 1, /* threads are not supported */
            threadsInConfig = 3,
        )
    }
}
