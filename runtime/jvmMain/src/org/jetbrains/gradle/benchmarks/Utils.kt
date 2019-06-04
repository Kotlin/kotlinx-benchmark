package org.jetbrains.gradle.benchmarks

import java.io.*

actual fun Double.format(precision: Int): String {
    return "%,.0${precision}f".format(this) //text.replace(Regex("\\B(?=(\\d{3})+(?!\\d))"), ",")
}

actual fun saveReport(reportFile: String?, results: Collection<ReportBenchmarkResult>) {
    if (reportFile == null)
        return

    File(reportFile).writeText(results.toJson())
}