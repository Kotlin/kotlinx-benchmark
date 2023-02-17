package kotlinx.benchmark

internal actual fun Double.format(precision: Int, useGrouping: Boolean): String {
    val options = js("{maximumFractionDigits:2, minimumFractionDigits:2, useGrouping:true}")
    options.minimumFractionDigits = precision
    options.maximumFractionDigits = precision
    options.useGrouping = useGrouping
    return this.asDynamic().toLocaleString(undefined, options) as String
}

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

internal actual inline fun measureNanoseconds(block: () -> Unit): Long =
    when {
        isD8 -> standaloneJsVmMeasureTime(block)
        isSpiderMonkey -> standaloneJsVmMeasureTime(block)
        isNodeJs -> nodeJsMeasureTime(block)
        else -> error("Unsupported js engine")
    }