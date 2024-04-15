package kotlinx.benchmark

@JsFun("(d, precision, useGrouping) => d.toLocaleString('en-GB', { maximumFractionDigits: precision, minimumFractionDigits: precision, useGrouping: useGrouping } )")
private external fun format(d: Double, precision: Int, useGrouping: Boolean): String

internal actual fun Double.format(precision: Int, useGrouping: Boolean): String = format(this, precision, useGrouping)

internal actual fun String.writeFile(text: String): Unit = jsEngineSupport.writeFile(this, text)

internal actual fun String.readFile(): String = jsEngineSupport.readFile(this)

internal abstract class JsEngineSupport {
    abstract fun writeFile(path: String, text: String)

    abstract fun readFile(path: String): String

    abstract fun arguments(): Array<out String>
}

@JsFun("() => typeof d8 !== 'undefined'")
internal external fun isD8Impl(): Boolean

internal val isD8: Boolean by lazy {
    isD8Impl()
}

internal val jsEngineSupport: JsEngineSupport by lazy {
    if (isD8) D8EngineSupport else NodeJsEngineSupport
}

internal external interface ExternalInterfaceType

internal actual inline fun measureNanoseconds(block: () -> Unit): Long =
    if (isD8) d8MeasureTime(block) else nodeJsMeasureTime(block)
