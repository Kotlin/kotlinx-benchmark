package kotlinx.benchmark.integration

import kotlin.test.*

class InvalidTargetingTest : GradleTest() {

    @Test
    fun testWasmNodeJs() {
        val runner = project("invalid-target/wasm-nodejs", true)
        runner.runAndFail("wasmJsBenchmark") {
            assertOutputContains("The nodejs() environment is not supported for Kotlin/Wasm benchmarks. Please use d8().")
        }
    }

    @Test
    fun testWasmBrowser() {
        val runner = project("invalid-target/wasm-browser", true)
        runner.runAndFail("wasmJsBenchmark") {
            assertOutputContains("The browser() environment is not supported for Kotlin/Wasm benchmarks. Please use d8().")
        }
    }

    @Test
    fun testJsD8() {
        val runner = project("invalid-target/js-d8", true)
        runner.runAndFail("jsBenchmark") {
            assertOutputContains("The d8() environment is not supported for Kotlin/JS benchmarks. Please use nodejs().")
        }
    }

    @Test
    fun testJsLegacyBackend() {
        val runner = project("invalid-target/js-legacy", true)
        runner.runAndFail("jsBenchmark") {
            assertOutputContains("Legacy Kotlin/JS backend is not supported. Please migrate to the Kotlin/JS IR compiler backend.")
        }
    }

    @Test
    fun testJsBrowser() {
        val runner = project("invalid-target/js-browser", true)
        runner.runAndFail("jsBenchmark") {
            assertOutputContains("The browser() environment is not supported for Kotlin/JS benchmarks. Please use nodejs().")
        }
    }
}
