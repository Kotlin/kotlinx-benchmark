package kotlinx.benchmark.js

import kotlinx.benchmark.CommonSuitExecutor
import kotlinx.benchmark.jsEngineSupport

class JsSimpleExecutor(
    name: String,
    @Suppress("UNUSED_PARAMETER") dummy_args: Array<out String>
) : CommonSuitExecutor(name, jsEngineSupport.arguments()[0])