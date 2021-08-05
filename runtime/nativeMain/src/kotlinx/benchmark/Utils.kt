package kotlinx.benchmark

import kotlinx.benchmark.native.NativeExecutor
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

internal fun writeFile(filePath: String, text: String) {
    val file = fopen(filePath, "w")
    try {
        if (fputs(text, file) == EOF) throw Error("File write error")
    } finally {
        fclose(file)
    }
}

actual fun String.readFile(): String = buildString {
    val file = fopen(this@readFile, "rb")
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

internal fun String.parseBenchmarkConfig(): NativeExecutor.BenchmarkRun {
    fun String.getElement(name: String) =
        if (startsWith(name)) {
            substringAfter("$name: ")
        } else throw NoSuchElementException("Parameter `$name` is required.")

    val content = readFile()
    val lines = content.lines().filter { it.isNotEmpty() }
    require(lines.size == 3, { "Wrong format of detailed benchmark configuration file. "})
    val name = lines[0].getElement("benchmark")
    val configuration = BenchmarkConfiguration.parse(lines[1].getElement("configuration"))
    val parameters = lines[2].getElement("parameters").parseMap()
    return NativeExecutor.BenchmarkRun(name, configuration, parameters)
}
