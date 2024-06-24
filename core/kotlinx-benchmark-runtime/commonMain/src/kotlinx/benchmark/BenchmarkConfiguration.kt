package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@KotlinxBenchmarkRuntimeInternalApi
class BenchmarkConfiguration private constructor(
    val iterations: Int,
    val warmups: Int,
    val iterationTime: Long,
    val iterationTimeUnit: BenchmarkTimeUnit,
    val outputTimeUnit: BenchmarkTimeUnit,
    val mode: Mode,
    val advanced: Map<String, String>,
) {
    constructor(runner: RunnerConfiguration, suite: SuiteDescriptor<*>) : this(
        iterations = runner.iterations ?: suite.iterations,
        warmups = runner.warmups ?: suite.warmups,
        iterationTime = runner.iterationTime ?: suite.iterationTime.value,
        iterationTimeUnit = runner.iterationTimeUnit ?: suite.iterationTime.timeUnit,
        outputTimeUnit = runner.outputTimeUnit ?: suite.outputTimeUnit,
        mode = runner.mode ?: suite.mode,
        advanced = runner.advanced
    )

    override fun toString() =
        "iterations=$iterations, warmups=$warmups, iterationTime=$iterationTime, " +
                "iterationTimeUnit=${iterationTimeUnit.toText()}, outputTimeUnit=${outputTimeUnit.toText()}, " +
                "mode=${mode.toText()}" +
                advanced.entries.joinToString(prefix = ", ", separator = ", ") { "advanced:${it.key}=${it.value}" }

    @KotlinxBenchmarkRuntimeInternalApi
    companion object {
        fun parse(description: String): BenchmarkConfiguration {
            val parameters = description.parseMap()
            fun getParameterValue(key: String) =
                parameters[key] ?: throw NoSuchElementException("Parameter `$key` is required.")

            val advanced = parameters
                .filter { it.key.startsWith("advanced:") }
                .entries
                .associate {
                    val advancedKey = it.key.substringAfter(":")
                    check(advancedKey.isNotEmpty()) { "Invalid advanced key - should not be empty" }
                    advancedKey to it.value
                }

            return BenchmarkConfiguration(
                iterations = getParameterValue("iterations").toInt(),
                warmups = getParameterValue("warmups").toInt(),
                iterationTime = getParameterValue("iterationTime").toLong(),
                iterationTimeUnit = parseTimeUnit(getParameterValue("iterationTimeUnit")),
                outputTimeUnit = parseTimeUnit(getParameterValue("outputTimeUnit")),
                mode = getParameterValue("mode").toMode(),
                advanced = advanced
            )
        }
    }
}

internal fun String.parseMap(): Map<String, String> =
    removeSurrounding("{", "}")
        .split(", ")
        .filter { it.isNotEmpty() }
        .associate {
            val keyValue = it.split("=")
            require(keyValue.size == 2) { "Wrong format of map string description!" }
            val (key, value) = keyValue
            key to value
        }