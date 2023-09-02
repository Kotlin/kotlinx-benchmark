package kotlinx.benchmark

import kotlin.time.DurationUnit
import kotlin.time.toDuration

@JsModule("fs")
@JsNonModule
private external object NodeFileSystem {
    fun writeFileSync(path: String, data: String)
    fun readFileSync(path: String, options: String): String
}

private object NodeJsEngineSupport : BenchmarkEngineSupport() {
    override fun writeFile(path: String, content: String) =
        NodeFileSystem.writeFileSync(path, content)

    override fun readFile(path: String): String =
        NodeFileSystem.readFileSync(path, "utf8")

    override fun arguments(): Array<out String> {
        val arguments = js("process.argv.slice(2).join(' ')") as String
        return arguments.split(' ').toTypedArray()
    }

    override fun getMeasurer(): Measurer = NodeJsMeasurer()

    override fun isSupported(): Boolean = isNodeJsEngine()
}

private fun hrTimeToNs(hrTime: dynamic): Long = (hrTime as Array<Double>).let { (seconds, nanos) ->
    seconds.toDuration(DurationUnit.SECONDS) + nanos.toDuration(DurationUnit.NANOSECONDS) }.inWholeNanoseconds

private class NodeJsMeasurer : Measurer() {
    val process = js("process")
    private var start: dynamic = 0.0
    override fun measureStart() {
        start = process.hrtime()
    }

    override fun measureFinish(): Long {
        val end = process.hrtime()
        return hrTimeToNs(end) - hrTimeToNs(start)
    }
}

private fun isNodeJsEngine(): Boolean =
    js("(typeof process !== 'undefined') && (process.release.name === 'node')") as Boolean

internal actual var engineSupport: BenchmarkEngineSupport =
    NodeJsEngineSupport