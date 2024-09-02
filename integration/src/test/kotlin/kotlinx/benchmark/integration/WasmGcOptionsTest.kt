package kotlinx.benchmark.integration

import org.junit.Test

// TODO: Remove this test and the associated code that adds the "--experimental-wasm-gc" flag once
//  the minimum supported Kotlin version no longer supports NodeJs < 22.0.0 and D8 < 12.3.68.
class WasmGcOptionsTest : GradleTest() {
    @Test
    fun nodeJs() {
        // The default NodeJs version used with the current kotlin_version is 22.0.0 (or newer).
        // Therefore, the test project intentionally uses an older NodeJs version.
        val runner = project(
            "wasm-gc-non-experimental/wasm-nodejs",
            print = true,
            kotlinVersion = GradleTestVersion.MinSupportedKotlinVersion.versionString
        )
        runner.runAndSucceed("wasmJsBenchmark")
    }

    @Test
    fun d8() {
        // The default D8 version used with the current kotlin_version is 11.9.85 (or newer).
        // Therefore, the test project intentionally uses a version newer than 12.3.68.
        val runner = project(
            "wasm-gc-non-experimental/wasm-d8",
            print = true,
            kotlinVersion = GradleTestVersion.MinSupportedKotlinVersion.versionString
        )
        runner.runAndSucceed( "wasmJsBenchmark")
    }
}
