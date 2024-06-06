package kotlinx.benchmark.integration

import org.junit.Test
import kotlin.test.Ignore

class WasmGcOptionsTest : GradleTest() {
    @Test
    @Ignore("Will be fixed in master, remove @Ignore after migration to 2.0")
    fun nodeJs() {
        // The test uses Kotlin 1.9.24 as previous versions
        // would append the --experimental-wasm-gc flag causing run failures.
        val runner = project(
            "wasm-gc-non-experimental/wasm-nodejs", true, kotlinVersion = "1.9.24"
        )
        runner.run("wasmJsBenchmark")
    }

    @Test
    @Ignore("Will be fixed in master, remove @Ignore after migration to 2.0")
    fun d8() {
        // The test uses Kotlin 1.9.24 as previous versions
        // would append the --experimental-wasm-gc flag causing run failures.
        val runner = project(
            "wasm-gc-non-experimental/wasm-d8", true, kotlinVersion = "1.9.24"
        )
        runner.run( "wasmJsBenchmark")
    }
}
