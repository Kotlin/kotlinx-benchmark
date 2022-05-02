package kotlinx.benchmark

actual fun Double.format(precision: Int, useGrouping: Boolean): String {
    val options = js("{maximumFractionDigits:2, minimumFractionDigits:2, useGrouping:true}")
    options.minimumFractionDigits = precision
    options.maximumFractionDigits = precision
    options.useGrouping = useGrouping
    return this.asDynamic().toLocaleString(undefined, options) as String
}

actual fun String.writeFile(text: String): Unit = jsEngineSupport.writeFile(this, text)

actual fun String.readFile(): String = jsEngineSupport.readFile(this)

internal abstract class JsEngineSupport {
    abstract fun writeFile(path: String, text: String)

    abstract fun readFile(path: String): String

    abstract fun arguments(): Array<out String>
}

internal fun isD8(): Boolean =
    js("typeof d8 !== 'undefined'") as Boolean

internal val jsEngineSupport: JsEngineSupport by lazy {
    if (isD8()) D8EngineSupport else NodeJsEngineSupport
}