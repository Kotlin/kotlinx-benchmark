package kotlinx.benchmark.wasm

import kotlinx.benchmark.*

class WasmBuiltInExecutor(
    name: String,
    @Suppress("UNUSED_PARAMETER") dummy_args: Array<out String>
) : CommonSuiteExecutor(name, jsEngineSupport.arguments()[0])