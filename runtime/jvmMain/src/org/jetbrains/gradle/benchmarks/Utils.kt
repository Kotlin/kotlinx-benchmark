package org.jetbrains.gradle.benchmarks

import java.io.*

actual fun Double.format(precision: Int): String {
    val text = "%.${precision}f".format(this)
    return text.replace(Regex("\\B(?=(\\d{3})+(?!\\d))"), ",")
}

actual fun saveReport(reportFile: String?, results: Collection<ReportBenchmarkResult>) {
    if (reportFile == null) 
        return
    
    File(reportFile).writeText(results.toJson())
}