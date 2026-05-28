package kotlinx.benchmark.integration

import kotlin.test.*

class CustomEngineTest : GradleTest() {

    @Test
    fun wasmJsCustomEngine() {
        val runner = project("wasm-custom-engine")

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
        }
    }
}