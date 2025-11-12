package kotlinx.benchmark

import kotlin.time.DurationUnit
import kotlin.time.toDuration

@JsModule("fs")
external object fs {
    fun readFileSync(path: String, options: WriteFileOptions): String
    fun writeFileSync(path: String, data: String, options: WriteFileOptions)
}

external interface WriteFileOptions : JsAny {
    var encoding: String?
    var mode: String?
    var flag: String?
}

fun writeFileOptions(block: WriteFileOptions.() -> Unit): WriteFileOptions =
    js("{}").unsafeCast<WriteFileOptions>().apply(block)

//@JsFun("(path, text) => require('fs').writeFileSync(path, text, 'utf8')")
//private external fun nodeJsWriteFile(path: String, text: String)
//
//@JsFun("(path) => require('fs').readFileSync(path, 'utf8')")
//private external fun nodeJsReadFile(path: String): String
//
@JsFun("() => process.argv.slice(2).join(' ')")
private external fun nodeJsArguments(): String

internal object NodeJsEngineSupport : JsEngineSupport() {
    override fun writeFile(path: String, text: String) =
        fs.writeFileSync(path, text, writeFileOptions { encoding = "utf8" })

    override fun readFile(path: String): String =
        fs.readFileSync(path, writeFileOptions { encoding = "utf8" })

    override fun arguments(): Array<out String> =
        nodeJsArguments().split(' ').toTypedArray()
}

private fun hrTimeToNs(hrTime: ExternalInterfaceType): Long {
    val fromSeconds = getArrayElement(hrTime, 0).toDuration(DurationUnit.SECONDS)
    val fromNanos = getArrayElement(hrTime, 1).toDuration(DurationUnit.NANOSECONDS)
    return (fromSeconds + fromNanos).inWholeNanoseconds
}

@JsFun("() => process")
private external fun getProcess(): ExternalInterfaceType

@JsFun("(process) => process.hrtime()")
private external fun getHrTime(process: ExternalInterfaceType): ExternalInterfaceType

@JsFun("(array, i) => array[i]")
private external fun getArrayElement(array: ExternalInterfaceType, i: Int): Double

internal inline fun nodeJsMeasureTime(block: () -> Unit): Long {
    val process = getProcess()
    val start = getHrTime(process)
    block()
    val end = getHrTime(process)
    return hrTimeToNs(end) - hrTimeToNs(start)
}