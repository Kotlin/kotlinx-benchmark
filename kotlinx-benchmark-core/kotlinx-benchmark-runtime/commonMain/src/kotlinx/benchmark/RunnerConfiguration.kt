package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@KotlinxBenchmarkRuntimeInternalApi
class RunnerConfiguration(config: String) {

    private val values = config.lines().groupBy({
        it.substringBefore(":")
    }, { it.substringAfter(":", "") })

    val name = singleValue("name")
    val reportFile = singleValue("reportFile")
    val traceFormat = singleValue("traceFormat")
    val reportFormat = singleValue("reportFormat", "json")

    val params = mapValues(
        "param", "="
    )

    val include = listValues("include")
    val exclude = listValues("exclude")

    val iterations = singleValueOrNull("iterations") { it.toInt() }
    val warmups = singleValueOrNull("warmups") { it.toInt() }
    val iterationTime = singleValueOrNull("iterationTime") { it.toLong() }
    val iterationTimeUnit = singleValueOrNull("iterationTimeUnit") { parseTimeUnit(it) }
    val advanced = mapSingleValues("advanced", "=")

    val outputTimeUnit = singleValueOrNull(
        "outputTimeUnit"
    ) { parseTimeUnit(it) }

    private fun <T> singleValueOrNull(name: String, map: (String) -> T): T? =
        singleValueOrNull(name)?.let(map)

    private fun singleValueOrNull(name: String): String? {
        val values = values[name] ?: return null
        return values.single()
    }

    private fun singleValue(name: String): String {
        return singleValueOrNull(name) ?: throw NoSuchElementException("Parameter `$name` is required.")
    }

    private fun singleValue(name: String, default: String): String {
        return singleValueOrNull(name) ?: default
    }

    private fun mapValues(name: String, delimiter: String): Map<String, List<String>> {
        val values = values[name] ?: return emptyMap()
        return values.groupBy({ it.substringBefore(delimiter) }, { it.substringAfter(delimiter) })
    }

    private fun mapSingleValues(name: String, delimiter: String): Map<String, String> = values[name]
        ?.associate {
            val splitted = it.split(delimiter)
            check(splitted.size == 2) { "Parameter name and value format is required for $name." }
            splitted[0] to splitted[1]
        } ?: emptyMap()

    private fun listValues(name: String): List<String> {
        return this.values[name] ?: emptyList()
    }

    val mode = singleValueOrNull(
        "mode"
    ) { it.toMode() }

    override fun toString(): String {
        return """$name -> $reportFile ($traceFormat, $reportFormat)
params: ${params.entries.joinToString(prefix = "{", postfix = "}") { "${it.key}: ${it.value}" }}
include: $include
exclude: $exclude
iterations: $iterations            
warmups: $warmups            
iterationTime: $iterationTime            
iterationTimeUnit: $iterationTimeUnit            
outputTimeUnit: $outputTimeUnit            
mode: $mode
advanced: $advanced
"""
    }
}

internal fun parseTimeUnit(text: String) = when (text) {
    BenchmarkTimeUnit.SECONDS.name, "s", "sec" -> BenchmarkTimeUnit.SECONDS
    BenchmarkTimeUnit.MICROSECONDS.name, "us", "micros" -> BenchmarkTimeUnit.MICROSECONDS
    BenchmarkTimeUnit.MILLISECONDS.name, "ms", "millis" -> BenchmarkTimeUnit.MILLISECONDS
    BenchmarkTimeUnit.NANOSECONDS.name, "ns", "nanos" -> BenchmarkTimeUnit.NANOSECONDS
    BenchmarkTimeUnit.MINUTES.name, "m", "min" -> BenchmarkTimeUnit.MINUTES
    else -> throw UnsupportedOperationException("Unknown time unit: $text")
}