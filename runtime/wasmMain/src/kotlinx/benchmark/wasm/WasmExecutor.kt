package kotlinx.benchmark.wasm

import kotlinx.benchmark.*

class WasmExecutor(
    name: String,
    @Suppress("UNUSED_PARAMETER") dummy_args: Array<out String>
) : CommonSuitExecutor(name, jsEngineSupport.arguments()[0])