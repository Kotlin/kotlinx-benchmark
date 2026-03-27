package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import kotlinx.benchmark.native.NativeExecutor
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.time.Duration
import kotlin.time.DurationUnit

internal actual fun Double.format(precision: Int, useGrouping: Boolean): String {
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

internal actual fun String.writeFile(text: String) {
    val file = fopen(this, "w")
    try {
        if (fputs(text, file) == EOF) throw Error("File write error")
    } finally {
        fclose(file)
    }
}

internal actual fun String.readFile(): String = buildString {
    val file = fopen(this@readFile, "rb")
    try {
        memScoped {
            while (true) {
                val bufferLength = 64 * 1024
                val buffer = allocArray<ByteVar>(bufferLength)
                val line = fgets(buffer, bufferLength, file)?.toKString() // newline symbol is included
                if (line.isNullOrEmpty()) break
                append(line)
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
    require(lines.size == 3) { "Wrong format of detailed benchmark configuration file. " }
    val name = lines[0].getElement("benchmark")
    val configuration = BenchmarkConfiguration.parse(lines[1].getElement("configuration"))
    val parameters = lines[2].getElement("parameters").parseMap()
    return NativeExecutor.BenchmarkRun(name, configuration, parameters)
}

internal actual inline fun measureNanoseconds(block: () -> Unit): Long = TODO("Not implemented for this platform")

// Iteration results for a single thread
internal class IterationResult(val operations: Long)

internal class AggregateIterationResult(
    val duration: Duration,
    val operations: LongArray
)

// Iteration results for all threads
internal fun AggregateIterationResult.nanosToText(mode: Mode, unit: BenchmarkTimeUnit): String {
    val value = nanosToSample(mode, unit)
    return when (mode) {
        Mode.Throughput -> "${value.formatSignificant(6)} ops/${unit.toText()}"
        Mode.AverageTime -> "${value.formatSignificant(6)} ${unit.toText()}/op"
        else -> throw UnsupportedOperationException("$mode is not supported")
    }
}

internal fun AggregateIterationResult.nanosToSample(mode: Mode, unit: BenchmarkTimeUnit): Double {
    val totalDuration = duration.toDouble(unit.toDurationUnit())
    return when (mode) {
        Mode.Throughput -> operations.sumOf { it.toDouble() } / totalDuration
        Mode.AverageTime -> operations.sumOf { totalDuration / it.toDouble()} / operations.size
        else -> throw UnsupportedOperationException("$mode is not supported")
    }
}

internal fun BenchmarkTimeUnit.toDurationUnit(): DurationUnit = when (this) {
    BenchmarkTimeUnit.MINUTES -> DurationUnit.MINUTES
    BenchmarkTimeUnit.SECONDS -> DurationUnit.SECONDS
    BenchmarkTimeUnit.MILLISECONDS -> DurationUnit.MILLISECONDS
    BenchmarkTimeUnit.MICROSECONDS -> DurationUnit.MICROSECONDS
    BenchmarkTimeUnit.NANOSECONDS -> DurationUnit.NANOSECONDS
    else -> throw IllegalStateException("Unsupported unit $this")
}
