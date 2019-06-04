package org.jetbrains.gradle.benchmarks

import kotlinx.cinterop.*
import platform.posix.*

actual fun Double.format(precision: Int): String {
    val longPart = toLong()
    val fractional = this@format - longPart
    val thousands = longPart.toString().replace(Regex("\\B(?=(\\d{3})+(?!\\d))"), ",")
    if (fractional == 0.0 || precision == 0)
        return thousands
    
    return memScoped {
        val bytes = allocArray<ByteVar>(100)
        sprintf(bytes, "%.${precision}F", fractional)
        val fractionText = bytes.toKString()
        return thousands + fractionText.removePrefix("0")
    }
}

actual fun saveReport(reportFile: String?, results: Collection<ReportBenchmarkResult>) {
    if (reportFile == null)
        return

    val file = fopen(reportFile, "w")
    fputs(results.toJson(), file)
    fclose(file)
}