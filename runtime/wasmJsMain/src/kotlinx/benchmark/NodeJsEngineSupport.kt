package kotlinx.benchmark

import kotlin.time.DurationUnit
import kotlin.time.toDuration

@JsFun("""
    (globalThis.module = (typeof process !== 'undefined') && (process.release.name === 'node') ?
        await import(/* webpackIgnore: true */'node:module') : void 0, () => {})
""")
private external fun persistModule()

private fun getRequire(): JsAny =
    js("""{ 
    const importMeta = import.meta;
    return globalThis.module.default.createRequire(importMeta.url);
}""")

private fun nodeJsWriteFile(require: JsAny, path: String, text: String): Unit =
    js("require('fs').writeFileSync(path, text, 'utf8')")

private fun nodeJsReadFile(require: JsAny, path: String): String =
    js("require('fs').readFileSync(path, 'utf8')")

private fun nodeJsArguments(): String =
    js("process.argv.slice(2).join(' ')")

private object NodeJsEngineSupport : BenchmarkEngineSupport() {
    private val require by lazy { persistModule().let { getRequire() } }

    override fun writeFile(path: String, content: String) =
        nodeJsWriteFile(require, path, content)

    override fun readFile(path: String): String =
        nodeJsReadFile(require, path)

    override fun arguments(): Array<out String> =
        nodeJsArguments().split(' ').toTypedArray()

    override fun getMeasurer(): Measurer = NodeJsMeasurer()

    override fun isSupported(): Boolean = isNodeJsEngine()
}

private fun hrTimeToNs(hrTime: JsArray<JsNumber>): Long {
    val fromSeconds = hrTime[0]!!.toDouble().toDuration(DurationUnit.SECONDS)
    val fromNanos = hrTime[1]!!.toDouble().toDuration(DurationUnit.NANOSECONDS)
    return (fromSeconds + fromNanos).inWholeNanoseconds
}

private fun getProcess(): Process = js("process")

private external interface Process {
    fun hrtime(): JsArray<JsNumber>
}

private class NodeJsMeasurer: Measurer() {
    private val process = getProcess()
    private var start: JsArray<JsNumber>? = null
    override fun measureStart() {
        start = process.hrtime()
    }

    override fun measureFinish(): Long {
        val end = process.hrtime()
        return hrTimeToNs(end) - hrTimeToNs(start!!)
    }
}

private fun isNodeJsEngine(): Boolean =
    js("(typeof process !== 'undefined') && (process.release.name === 'node')")

internal actual var engineSupport: BenchmarkEngineSupport =
    NodeJsEngineSupport