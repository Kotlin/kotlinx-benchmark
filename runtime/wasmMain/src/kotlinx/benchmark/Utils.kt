package kotlinx.benchmark

@JsFun("(d, precision, useGrouping) => d.toLocaleString(undefined, { maximumFractionDigits: precision, minimumFractionDigits: precision, useGrouping: useGrouping } )")
private external fun format(d: Double, precision: Int, useGrouping: Boolean): String

internal actual fun Double.format(precision: Int, useGrouping: Boolean): String = format(this, precision, useGrouping)

internal actual fun String.writeFile(text: String): Unit = jsEngineSupport.writeFile(this, text)

internal actual fun String.readFile(): String = jsEngineSupport.readFile(this)

internal abstract class JsEngineSupport {
    abstract fun writeFile(path: String, text: String)

    abstract fun readFile(path: String): String

    abstract fun arguments(): Array<out String>
}

internal val isD8: Boolean by lazy { isD8Engine() }

internal val isSpiderMonkey: Boolean by lazy { isSpiderMonkeyEngine() }

internal val isNodeJs: Boolean by lazy { isNodeJsEngine() }

internal val jsEngineSupport: JsEngineSupport by lazy {
    when {
        isD8 -> D8EngineSupport
        isSpiderMonkey -> SpiderMonkeyEngineSupport
        isNodeJs -> NodeJsEngineSupport
        else -> error("Unsupported js engine")
    }
}

internal external interface ExternalInterfaceType

internal actual inline fun measureNanoseconds(block: () -> Unit): Long =
    when {
        isD8 -> standaloneJsVmMeasureTime(block)
        isSpiderMonkey -> standaloneJsVmMeasureTime(block)
        isNodeJs -> nodeJsMeasureTime(block)
        else -> error("Unsupported js engine")
    }