package kotlinx.benchmark

import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal external fun require(module: String): dynamic

internal object NodeJsEngineSupport : JsEngineSupport() {
    override fun writeFile(path: String, text: String) =
        require("fs").writeFileSync(path, text)

    override fun readFile(path: String): String =
        require("fs").readFileSync(path, "utf8") as String

    override fun arguments(): Array<out String> {
        val arguments = js("process.argv.slice(2).join(' ')") as String
        return arguments.split(' ').toTypedArray()
    }
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