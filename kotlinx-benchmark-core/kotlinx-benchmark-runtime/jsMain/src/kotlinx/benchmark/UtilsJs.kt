package kotlinx.benchmark

internal actual fun Double.format(precision: Int, useGrouping: Boolean): String {
    val options = js("{maximumFractionDigits:2, minimumFractionDigits:2, useGrouping:true}")
    options.minimumFractionDigits = precision
    options.maximumFractionDigits = precision
    options.useGrouping = useGrouping
    return this.asDynamic().toLocaleString("en-GB", options) as String
}

internal actual fun String.writeFile(text: String): Unit = jsEngineSupport.writeFile(this, text)

internal actual fun String.readFile(): String = jsEngineSupport.readFile(this)

internal abstract class JsEngineSupport {
    abstract fun writeFile(path: String, text: String)

    abstract fun readFile(path: String): String

    abstract fun arguments(): Array<out String>
}

internal val isD8: Boolean by lazy {
    js("typeof d8 !== 'undefined'") as Boolean
}

internal val jsEngineSupport: JsEngineSupport by lazy {
    if (isD8) D8EngineSupport else NodeJsEngineSupport
}

internal actual inline fun measureNanoseconds(block: () -> Unit): Long =
    if (isD8) d8MeasureTime(block) else nodeJsMeasureTime(block)
