package kotlinx.benchmark.integration

import kotlin.test.Test

class KotlinWasmTest : GradleTest() {

    @Test
    fun wasmJsBenchmark() = assertTasksExecuted("Js")

    @Test
    fun wasmWasiBenchmark() = assertTasksExecuted("Wasi")

    private fun assertTasksExecuted(target: String) {
        val runner = project("kotlin-multiplatform")

        runner.runAndSucceed("wasm${target}BenchmarkDevelopmentExecutableBenchmark") {
            assertTasksExecuted(":compileWasm${target}BenchmarkDevelopmentExecutableKotlinWasm${target}")
            assertTaskNotExecuted(":compileWasm${target}BenchmarkDevelopmentExecutableKotlinWasm${target}Optimize")
        }

        runner.runAndSucceed("wasm${target}BenchmarkProductionExecutableBenchmark") {
            assertTasksExecuted(
                ":compileWasm${target}BenchmarkProductionExecutableKotlinWasm${target}",
                ":compileWasm${target}BenchmarkProductionExecutableKotlinWasm${target}Optimize",
            )
        }
    }
}
