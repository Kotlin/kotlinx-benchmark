package kotlinx.benchmark

import kotlin.time.Duration.Companion.milliseconds

@JsFun("(path) => globalThis.read(path)")
private external fun browserEngineReadFile(path: String): String

internal abstract class StandaloneJsVmSupport : JsEngineSupport() {
    override fun writeFile(path: String, text: String) =
        print("<FILE:$path>$text<ENDFILE>")

    override fun readFile(path: String): String =
        browserEngineReadFile(path)
}

@JsFun("() => (typeof self !== 'undefined' ? self : globalThis).performance")
private external fun getPerformance(): ExternalInterfaceType

@JsFun("(performance) => performance.now()")
private external fun performanceNow(performance: ExternalInterfaceType): Double

internal inline fun standaloneJsVmMeasureTime(block: () -> Unit): Long {
    val performance = getPerformance()
    val start = performanceNow(performance)
    block()
    val end = performanceNow(performance)
    val startInNs = start.milliseconds.inWholeNanoseconds
    val endInNs = end.milliseconds.inWholeNanoseconds
    return endInNs - startInNs
}