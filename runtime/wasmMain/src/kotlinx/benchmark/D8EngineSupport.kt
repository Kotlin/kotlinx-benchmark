package kotlinx.benchmark

import kotlin.time.Duration.Companion.milliseconds

@JsFun("(path) => globalThis.read(path)")
private external fun d8ReadFile(path: String): String

@JsFun("() => globalThis.arguments.join(' ')")
private external fun d8Arguments(): String

internal object D8EngineSupport : JsEngineSupport() {
    override fun writeFile(path: String, text: String) {
        //WORKAROUND: D8 cannot write into files, this format will be parsed on gradle plugin side
        if (text.isEmpty()) {
            print("<FILE:$path><ENDFILE>")
        } else {
            print("<FILE:$path>")
            print(text)
            print("<ENDFILE>")
        }
    }

    override fun readFile(path: String): String =
        d8ReadFile(path)

    override fun arguments(): Array<out String> =
        d8Arguments().split(' ').toTypedArray()
}

@JsFun("() => (typeof self !== 'undefined' ? self : globalThis).performance")
private external fun getPerformance(): ExternalInterfaceType

@JsFun("(performance) => performance.now()")
private external fun performanceNow(performance: ExternalInterfaceType): Double

internal inline fun d8MeasureTime(block: () -> Unit): Long {
    val performance = getPerformance()
    val start = performanceNow(performance)
    block()
    val end = performanceNow(performance)
    val startInNs = start.milliseconds.inWholeNanoseconds
    val endInNs = end.milliseconds.inWholeNanoseconds
    return endInNs - startInNs
}