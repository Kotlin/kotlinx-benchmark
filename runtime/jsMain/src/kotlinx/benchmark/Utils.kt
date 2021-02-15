package kotlinx.benchmark

actual fun Double.format(precision: Int, useGrouping: Boolean): String {
    val options = js("{maximumFractionDigits:2, minimumFractionDigits:2, useGrouping:true}")
    options.minimumFractionDigits = precision
    options.maximumFractionDigits = precision
    options.useGrouping = useGrouping
    return this.asDynamic().toLocaleString(undefined, options)
}

private val fs = kotlinx.benchmark.js.require("fs")

actual fun saveReport(reportFile: String, report: String) {
    fs.writeFile(reportFile, report) { err -> if (err != null) throw err }
}

actual fun String.readFile(): String {
    return fs.readFileSync(this, "utf8")
}
