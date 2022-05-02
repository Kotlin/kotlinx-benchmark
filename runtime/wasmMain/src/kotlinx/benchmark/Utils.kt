package kotlinx.benchmark

@JsFun("(d, precision, useGrouping) => d.toLocaleString(undefined, { maximumFractionDigits: precision, minimumFractionDigits: precision, useGrouping: useGrouping } )")
private external fun format(d: Double, precision: Int, useGrouping: Boolean): String

actual fun Double.format(precision: Int, useGrouping: Boolean): String = format(this, precision, useGrouping)

actual fun String.writeFile(text: String): Unit = jsEngineSupport.writeFile(this, text)

actual fun String.readFile(): String = jsEngineSupport.readFile(this)

internal abstract class JsEngineSupport {
    abstract fun writeFile(path: String, text: String)

    abstract fun readFile(path: String): String

    abstract fun arguments(): Array<out String>
}

@JsFun("() => typeof d8 !== 'undefined'")
internal external fun isD8(): Boolean

internal val jsEngineSupport: JsEngineSupport by lazy {
    if (isD8()) D8EngineSupport else NodeJsEngineSupport
}