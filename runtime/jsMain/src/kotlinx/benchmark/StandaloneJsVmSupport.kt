package kotlinx.benchmark

import kotlin.time.Duration.Companion.milliseconds

private external fun read(path: String): String

internal abstract class StandaloneJsVmSupport : JsEngineSupport() {
    override fun readFile(path: String): String =
        read(path)
}

internal inline fun standaloneJsVmMeasureTime(block: () -> Unit): Long {
    val performance = js("(typeof self !== 'undefined' ? self : globalThis).performance")
    val start = performance.now()
    block()
    val end = performance.now()
    val startInNs = (start as Double).milliseconds.inWholeNanoseconds
    val endInNs = (end as Double).milliseconds.inWholeNanoseconds
    return endInNs - startInNs
}