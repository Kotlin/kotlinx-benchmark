package kotlinx.benchmark

import kotlinx.cinterop.*
import platform.posix.*

actual fun Double.format(precision: Int, useGrouping: Boolean): String {
    val longPart = toLong()
    val fractional = this - longPart
    val thousands =
        if (useGrouping) longPart.toString().replace(Regex("\\B(?=(\\d{3})+(?!\\d))"), ",")
        else longPart.toString()
    if (precision == 0)
        return thousands

    return memScoped {
        val bytes = allocArray<ByteVar>(100)
        sprintf(bytes, "%.${precision}f", fractional)
        val fractionText = bytes.toKString()
        thousands + fractionText.removePrefix("0")
    }
}

actual fun saveReport(reportFile: String, report: String) {
    val file = fopen(reportFile, "w")
    fputs(report, file)
    fclose(file)
}

actual fun String.readConfigFile(): String = buildString {
    val file = fopen(this@readConfigFile, "rb")
    try {
        memScoped {
            while (true) {
                val bufferLength = 64 * 1024
                val buffer = allocArray<ByteVar>(bufferLength)
                val line = fgets(buffer, bufferLength, file)?.toKString()
                if (line == null || line.isEmpty()) break
                appendLine(line)
            }
        }

    } finally {
        fclose(file)
    }
}
