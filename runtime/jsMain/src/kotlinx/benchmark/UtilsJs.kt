package kotlinx.benchmark

internal actual fun Double.format(precision: Int, useGrouping: Boolean): String {
    val options = js("{maximumFractionDigits:2, minimumFractionDigits:2, useGrouping:true}")
    options.minimumFractionDigits = precision
    options.maximumFractionDigits = precision
    options.useGrouping = useGrouping
    return this.asDynamic().toLocaleString("en-GB", options) as String
}