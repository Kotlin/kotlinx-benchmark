package kotlinx.benchmark.integration

import kotlin.test.*

class InvalidTargetingTest : GradleTest() {

    @Test
    fun testInvalidTargetForKotlinWASM() {
        val runner = project("kotlin-multiplatform") {
            configuration("invalidWasm") {
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                reportFormat = "json"
            }
            register("wasmTest")
            kotlin {
                wasm("wasmTest") {
                    nodejs()
                }
            }
        }              

        runner.runAndFail("invalidWasmBenchmark") {
            assertOutputContains("Kotlin/WASM does not support targeting NodeJS for benchmarks.")
        }
    }

    @Test
    fun testInvalidTargetForKotlinJS() {
        val runner = project("kotlin-multiplatform") {
            configuration("invalidJS") {
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                reportFormat = "json"
            }
            register("jsTest")
            kotlin {
                js("jsTest", KotlinConfiguration.IR) {
                    d8()
                }
            }
        }              

        runner.runAndFail("invalidJsBenchmark") {
            assertOutputContains("Kotlin/JS does not support targeting D8 for benchmarks.")
        }
    }
}
