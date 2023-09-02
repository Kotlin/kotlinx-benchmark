package kotlinx.benchmark

private fun format(d: Double, precision: Int, useGrouping: Boolean): String =
    js("d.toLocaleString('en-GB', { maximumFractionDigits: precision, minimumFractionDigits: precision, useGrouping: useGrouping } )")

internal actual fun Double.format(precision: Int, useGrouping: Boolean): String = format(this, precision, useGrouping)