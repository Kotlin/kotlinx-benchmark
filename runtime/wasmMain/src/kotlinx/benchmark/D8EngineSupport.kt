package kotlinx.benchmark

import kotlin.time.Duration.Companion.milliseconds

@JsFun("(path) => globalThis.read(path)")
private external fun d8ReadFile(path: String): String

@JsFun("() => globalThis.arguments.join(' ')")
private external fun d8Arguments(): String

private const val maxStringLength = 65_536 / 4

internal object D8EngineSupport : JsEngineSupport() {
    override fun writeFile(path: String, text: String) {
        //WORKAROUND: D8 cannot write into files, this format will be parsed on gradle plugin side
        if (text.isEmpty()) {
            print("<FILE:$path><ENDFILE>")
        } else {
            print("<FILE:$path>")
            //TODO("Workaround for kotlin/wasm issue in 1.7.20 and below. This should be removed for kotlin 1.8.0 or above")
            var srcStartIndex = 0
            var srcEndIndex = srcStartIndex + maxStringLength
            while (srcEndIndex < text.length) {
                print(text.substring(srcStartIndex, srcEndIndex))
                srcStartIndex = srcEndIndex
                srcEndIndex += maxStringLength
            }
            print(text.substring(srcStartIndex))
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