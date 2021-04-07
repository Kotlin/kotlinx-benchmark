package kotlinx.benchmark

import kotlin.math.*

sealed class BenchmarkReportFormatter {
    abstract fun format(results: Collection<ReportBenchmarkResult>): String

    companion object {
        fun create(format: String): BenchmarkReportFormatter = when (format.toLowerCase()) {
            "json" -> JsonBenchmarkReportFormatter
            "csv" -> CsvBenchmarkReportFormatter(",")
            "scsv" -> CsvBenchmarkReportFormatter(";")
            "text" -> TextBenchmarkReportFormatter
            else -> throw UnsupportedOperationException("Report format $format is not supported.")
        }
    }
}

internal object TextBenchmarkReportFormatter : BenchmarkReportFormatter() {
    private const val padding = 2

    override fun format(results: Collection<ReportBenchmarkResult>): String {
        fun columnLength(column: String, selector: (ReportBenchmarkResult) -> String): Int =
            max(column.length, results.maxOf { selector(it).length })

        val shortNames = denseBenchmarkNames(results.map { it.benchmark.name })
        val nameLength = columnLength("Benchmark") { shortNames[it.benchmark.name]!! }
        val paramNames = results.flatMap { it.params.keys }.toSet()
        val paramLengths = paramNames.associateWith { paramName ->
            max(paramName.length + 2, results.mapNotNull { it.params[paramName] }.maxOf { it.length }) + padding
        }

        val modeLength = columnLength("Mode") { it.config.mode.toText() } + padding
        val samplesLength = columnLength("Cnt") { it.values.size.toString() } + padding
        val scopeLength = columnLength("Score") { it.score.format(3, useGrouping = false) } + padding
        val errorLength = columnLength("Error") { it.error.format(3, useGrouping = false) } + padding - 1
        val unitsLength = columnLength("Units") { unitText(it.config.mode, it.config.outputTimeUnit) } + padding

        return buildString {
            appendPaddedAfter("Benchmark", nameLength)
            paramNames.forEach {
                appendPaddedBefore("($it)", paramLengths[it]!!)
            }
            appendPaddedBefore("Mode", modeLength)
            appendPaddedBefore("Cnt", samplesLength)
            appendPaddedBefore("Score", scopeLength)
            append("  ")
            appendPaddedBefore("Error", errorLength)
            appendPaddedBefore("Units", unitsLength)
            appendLine()

            results.forEach { result ->
                appendPaddedAfter(shortNames[result.benchmark.name]!!, nameLength)
                paramNames.forEach {
                    appendPaddedBefore(result.params[it] ?: "N/A", paramLengths[it]!!)
                }
                appendPaddedBefore(result.config.mode.toText(), modeLength)
                appendPaddedBefore(result.values.size.takeIf { it > 1 }?.toString() ?: " ", samplesLength)
                appendPaddedBefore(result.score.format(3, useGrouping = false), scopeLength)
                if (result.error.isNaNOrZero()) {
                    append("  ")
                    appendPaddedBefore("", errorLength)
                } else {
                    append(" \u00B1")
                    appendPaddedBefore(result.error.format(3, useGrouping = false), errorLength)
                }
                appendPaddedBefore(unitText(result.config.mode, result.config.outputTimeUnit), unitsLength)
                appendLine()
            }
        }
    }

    private fun StringBuilder.appendSpace(l: Int): StringBuilder = append(" ".repeat(l))

    private fun StringBuilder.appendPaddedBefore(value: String, l: Int): StringBuilder =
        appendSpace(l - value.length).append(value)

    private fun StringBuilder.appendPaddedAfter(value: String, l: Int): StringBuilder =
        append(value).appendSpace(l - value.length)


    /**
     * Algorithm:
     *  1. remove package names, if it is the same for all benchmarks
     *  2. if not, shorthand same package names
     *
     *  (jmh similar logic)
     */
    private fun denseBenchmarkNames(src: List<String>): Map<String, String> {
        if (src.isEmpty()) return emptyMap()

        var first = true
        var prefixCut = false

        val prefix = src.fold(emptyList<String>()) { prefix, s ->
            val names = s.split(".")
            if (first) {
                first = false
                names.takeWhile { it.toLowerCase() == it }
            } else {
                val common = prefix.zip(names).takeWhile { (p, n) -> p == n && n.toLowerCase() == n }
                if (prefix.size != common.size) prefixCut = true
                prefix.take(common.size)
            }
        }.map { if (prefixCut) it[0].toString() else "" }

        return src.associateWith { s ->
            val names = prefix + s.split(".").drop(prefix.size)
            names.joinToString("") { if (it.isNotEmpty()) "$it." else "" }.removeSuffix(".")
        }
    }
}

private class CsvBenchmarkReportFormatter(val delimiter: String) : BenchmarkReportFormatter() {
    override fun format(results: Collection<ReportBenchmarkResult>): String = buildString {
        val allParams = results.flatMap { it.params.keys }.toSet()
        appendHeader(allParams)
        results.forEach {
            appendResult(allParams, it)
        }
    }

    private fun StringBuilder.appendHeader(params: Set<String>) {
        appendEscaped("Benchmark").append(delimiter)
        appendEscaped("Mode").append(delimiter)
        appendEscaped("Threads").append(delimiter)
        appendEscaped("Samples").append(delimiter)
        appendEscaped("Score").append(delimiter)
        appendEscaped("Score Error (99.9%)").append(delimiter)
        appendEscaped("Unit")
        params.forEach {
            append(delimiter)
            appendEscaped("Param: $it")
        }
        append("\r\n")
    }

    private fun StringBuilder.appendResult(params: Set<String>, result: ReportBenchmarkResult) {
        appendEscaped(result.benchmark.name).append(delimiter)
        appendEscaped(result.config.mode.toText()).append(delimiter)
        append(1).append(delimiter)
        append(result.values.size).append(delimiter)
        append(result.score.format(6, useGrouping = false)).append(delimiter)
        append(result.error.format(6, useGrouping = false)).append(delimiter)
        appendEscaped(unitText(result.config.mode, result.config.outputTimeUnit))
        params.forEach {
            append(delimiter)
            result.params[it]?.let { param ->
                appendEscaped(param)
            }
        }
        append("\r\n")
    }

    private fun StringBuilder.appendEscaped(value: String): StringBuilder =
        append("\"").append(value.replace("\"", "\"\"")).append("\"")

}

object JsonBenchmarkReportFormatter : BenchmarkReportFormatter() {

    override fun format(results: Collection<ReportBenchmarkResult>): String =
        results.joinToString(",", prefix = "[", postfix = "\n]", transform = this::format)

    fun format(result: ReportBenchmarkResult): String =
        """
  {
    "benchmark" : "${result.benchmark.name}",
    "mode" : "${result.config.mode.toText()}",
    "warmupIterations" : ${result.config.warmups},
    "warmupTime" : "${result.config.iterationTime} ${result.config.iterationTimeUnit.toText()}",
    "measurementIterations" : ${result.config.iterations},
    "measurementTime" : "${result.config.iterationTime} ${result.config.iterationTimeUnit.toText()}",
    "params" : {
          ${result.params.entries.joinToString(separator = ",\n          ") { "\"${it.key}\" : \"${it.value}\"" }}
    },
    "nativeIterationMode" : "${result.config.nativeIterationMode.toText()}",
    "primaryMetric" : {
       "score": ${result.score},
       "scoreError": ${result.error},
       "scoreConfidence" : [
          ${result.confidence.first},
          ${result.confidence.second}
       ],
       "scorePercentiles" : {
          ${result.percentiles.entries.joinToString(separator = ",\n          ") { "\"${it.key.format(2)}\" : ${it.value}" }}
       },
       "scoreUnit" : "${unitText(result.config.mode, result.config.outputTimeUnit)}",
       "rawData" : [
           ${
            result.values.joinToString(
                prefix = "[\n             ",
                postfix = "\n           ]",
                separator = ",\n             "
            )
        }
       ]
    },
    "secondaryMetrics" : {
    }
  }"""

}
