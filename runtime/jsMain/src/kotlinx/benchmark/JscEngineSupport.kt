package kotlinx.benchmark

import kotlin.time.Duration.Companion.seconds

private fun jscArguments(): String =
    js("typeof globalThis.arguments !== 'undefined' ? globalThis.arguments.join(' ') : \"\"")

private fun jscPrint(text: String) =
    js("print(text)")

internal object JscEngineSupport : StandaloneJsVmSupport() {
    override fun writeFile(path: String, text: String) {
        jscPrint("<FILE:$path>$text<ENDFILE>")
    }

    override fun arguments(): Array<out String> =
        jscArguments().split(' ').toTypedArray()
}

internal fun isJscEngine(): Boolean {
    // 1. Evaluate the environment condition
    val res = js("typeof process === 'undefined' && typeof d8 === 'undefined' && typeof globalThis.isIon === 'undefined'") as Boolean

    // 2. Grab the global scope dynamically
    val global: dynamic = js("globalThis")

    // 3. Apply your fallbacks
    if (global.console == null) {
        global.console = js("{}")
    }
    if (res) {
        // Map console.log to the shell's native print function
        global.console.log = js("print")
    }

    return res
}

internal fun getPreciseTime(): Double =
    js("Number(preciseTime().toFixed(9))") as Double

internal inline fun jscMeasureTime(block: () -> Unit): Long {
    val start = getPreciseTime()
    block()
    val end = getPreciseTime()
    val startInNs = start.seconds.inWholeNanoseconds
    val endInNs = end.seconds.inWholeNanoseconds
    return endInNs - startInNs
}