package kotlinx.benchmark

import java.io.*

actual fun Double.format(precision: Int, useGrouping: Boolean): String {
    return if (useGrouping) "%,.0${precision}f".format(this)
    else "%.0${precision}f".format(this)
}

actual fun saveReport(reportFile: String, report: String) {
    File(reportFile).writeText(report)
}

actual fun String.readFile(): String {
    return File(this).readText()
}
