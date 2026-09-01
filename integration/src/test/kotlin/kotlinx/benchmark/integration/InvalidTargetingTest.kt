package kotlinx.benchmark.integration

import kotlin.test.*

class InvalidTargetingTest : GradleTest() {

    @Test
    fun testWasmNodeJs() {
        val runner = project("invalid-target/wasm-nodejs")
        runner.runAndSucceed("wasmJsBenchmark")
    }

    @Test
    fun testWasmBrowser() {
        val runner = project("invalid-target/wasm-browser")
        runner.runAndFail("wasmJsBenchmark") {
            assertOutputContains("kotlinx-benchmark only supports nodejs() environment for KotlinJs or Kotlin/Wasm.")
        }
    }

    @Test
    fun testJsBrowser() {
        val runner = project("invalid-target/js-browser")
        runner.runAndFail("jsBenchmark") {
            assertOutputContains("kotlinx-benchmark only supports nodejs() environment for KotlinJs or Kotlin/Wasm.")
        }
    }
}
