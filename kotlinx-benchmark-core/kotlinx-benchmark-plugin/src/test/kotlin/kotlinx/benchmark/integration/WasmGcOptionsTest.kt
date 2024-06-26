package kotlinx.benchmark.integration

import org.junit.Test

class WasmGcOptionsTest : GradleTest() {
    @Test
    fun nodeJs() {
        // The test uses Kotlin 1.9.24 as previous versions
        // would append the --experimental-wasm-gc flag causing run failures.
        val runner = project(
            "wasm-gc-non-experimental/wasm-nodejs", true, kotlinVersion = "1.9.24"
        )
        runner.run("wasmJsBenchmark")
    }

    @Test
    fun d8() {
        // The test uses Kotlin 1.9.24 as previous versions
        // would append the --experimental-wasm-gc flag causing run failures.
        val runner = project(
            "wasm-gc-non-experimental/wasm-d8", true, kotlinVersion = "1.9.24"
        )
        runner.run( "wasmJsBenchmark")
    }
}
