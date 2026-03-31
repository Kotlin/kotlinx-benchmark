package kotlinx.benchmark

import kotlin.time.DurationUnit
import kotlin.time.toDuration

private fun nodeJsWriteFile(path: String, text: String) =
    js("require('fs').writeFileSync(path, text, 'utf8')")

private fun nodeJsReadFile(path: String): String =
    js("require('fs').readFileSync(path, 'utf8')")

private fun nodeJsArguments(): String =
    js("process.argv.slice(2).join(' ')")

internal object NodeJsEngineSupport : JsEngineSupport() {
    override fun writeFile(path: String, text: String) =
        nodeJsWriteFile(path, text)

    override fun readFile(path: String): String =
        nodeJsReadFile(path)

    override fun arguments(): Array<out String> =
        nodeJsArguments().split(' ').toTypedArray()
}

private fun hrTimeToNs(hrTime: dynamic): Long = (hrTime as Array<Double>).let { (seconds, nanos) ->
    seconds.toDuration(DurationUnit.SECONDS) + nanos.toDuration(DurationUnit.NANOSECONDS) }.inWholeNanoseconds

internal inline fun nodeJsMeasureTime(block: () -> Unit): Long {
    val process = js("process")
    val start = process.hrtime()
    block()
    val end = process.hrtime()
    return hrTimeToNs(end) - hrTimeToNs(start)
}

internal fun isNodeJsEngine(): Boolean =
    js("(typeof process !== 'undefined') && (process.release.name === 'node')") as Boolean