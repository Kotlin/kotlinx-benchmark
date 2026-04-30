package kotlinx.benchmark

private fun format(d: Double, precision: Int, useGrouping: Boolean): String =
    js("d.toLocaleString('en-GB', { maximumFractionDigits: precision, minimumFractionDigits: precision, useGrouping: useGrouping } )")

internal actual fun Double.format(precision: Int, useGrouping: Boolean): String = format(this, precision, useGrouping)

internal fun nodeJsEngineBinaryPath(): String = js("process.argv[0]")

internal fun nodeJsEngineModulePath(): String = js("process.argv[1]")

internal fun nodeJsGetDirName(filePath: String): String = js("require('path').dirname(filePath)")