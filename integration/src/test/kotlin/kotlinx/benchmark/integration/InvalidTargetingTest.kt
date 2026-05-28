package kotlinx.benchmark.integration

import kotlin.test.*

class InvalidTargetingTest : GradleTest() {

    @Test
    fun testWasmNodeJs() {
        val runner = project("invalid-target/wasm-nodejs", true)
        runner.runAndSucceed("wasmJsBenchmark")
    }

    @Test
    fun testWasmBrowser() {
        val runner = project("invalid-target/wasm-browser", true)
        runner.runAndFail("wasmJsBenchmark") {
            assertOutputContains("kotlinx-benchmark only supports nodejs() environment for KotlinJs or Kotlin/Wasm.")
        }
    }

    @Test
    fun testJsBrowser() {
        val runner = project("invalid-target/js-browser", true)
        runner.runAndFail("jsBenchmark") {
            assertOutputContains("kotlinx-benchmark only supports nodejs() environment for KotlinJs or Kotlin/Wasm.")
        }
    }
}
