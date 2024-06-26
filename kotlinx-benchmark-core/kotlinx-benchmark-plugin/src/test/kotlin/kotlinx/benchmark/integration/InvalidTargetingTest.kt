package kotlinx.benchmark.integration

import kotlin.test.*

class InvalidTargetingTest : GradleTest() {

    @Test
    fun testWasmNodeJs() {
        val runner = project("invalid-target/wasm-nodejs", true)
        runner.run("wasmJsBenchmark") // Successful
    }

    @Test
    fun testWasmBrowser() {
        val runner = project("invalid-target/wasm-browser", true)
        runner.runAndFail("wasmJsBenchmark") {
            assertOutputContains("kotlinx-benchmark only supports d8() and nodejs() environments for Kotlin/Wasm.")
        }
    }

    @Test
    fun testJsD8() {
        val runner = project("invalid-target/js-d8", true)
        runner.runAndFail("jsBenchmark") {
            assertOutputContains("kotlinx-benchmark only supports nodejs() environment for Kotlin/JS.")
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
            assertOutputContains("kotlinx-benchmark only supports nodejs() environment for Kotlin/JS.")
        }
    }
}
