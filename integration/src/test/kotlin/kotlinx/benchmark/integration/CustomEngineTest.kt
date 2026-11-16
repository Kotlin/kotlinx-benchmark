package kotlinx.benchmark.integration

import kotlin.test.*

class CustomEngineTest : GradleTest() {

    @Test
    fun wasmJsCustomEngine() {
        // The project is compiled with -Xwasm-use-stack-switching-proposal (available since Kotlin 2.4.20)
        // and runs with `wasmFork = perBenchmark`: the spawned Node.js process must inherit the parent's
        // Node.js flags (process.execArgv), otherwise it fails to load the module without `--experimental-wasm-wasmfx`.
        val kotlinVersion = KotlinTestVersion.mostRecent(
            KotlinTestVersion.Kotlin2_4_20.versionString,
            KotlinTestVersion.overriddenVersion() ?: KotlinTestVersion.Kotlin2_4_20.versionString,
        )
        val runner = project("wasm-custom-engine", kotlinVersion = kotlinVersion)

        val spawningProcessMessage = "Spawning CustomNodeJs..."
        val customEngineMessages = listOf(
            "Custom engine registered",
            "Custom engine read from file",
            "Custom engine write to file",
            "Custom engine measurer start",
            "Custom engine measurer finish",
        )

        runner.runAndSucceed("wasmJsBenchmarkProductionExecutableBenchmark") {
            assertOutputContains(spawningProcessMessage)
            customEngineMessages.forEach(::assertOutputContains)
            assertOutputDoesNotContain("core stack switching not enabled")
        }
    }
}
