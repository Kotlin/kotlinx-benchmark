package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import kotlin.math.*

@KotlinxBenchmarkRuntimeInternalApi
class ReportBenchmarksStatistics(values: DoubleArray) {
    val values = values.sortedArray()
    val size: Int get() = values.size

    fun valueAt(quantile: Double): Double {
        if (quantile < 0.0 || quantile > 1.0 || quantile.isNaN())
            throw IllegalArgumentException("$quantile is not in [0..1]")

        if (size == 0) return 0.0

        val pos = quantile * (values.size + 1)
        val index = pos.toInt()
        return when {
            index < 1 -> values[0]
            index >= values.size -> values[values.size - 1]
            else -> {
                val lower = values[index - 1]
                val upper = values[index]
                lower + (pos - floor(pos)) * (upper - lower)
            }
        }
    }

    fun median() = valueAt(0.5)

    fun min(): Double = when (size) {
        0 -> 0.0
        else -> values[0]
    }

    fun max(): Double = when (size) {
        0 -> 0.0
        else -> values[values.lastIndex]
    }

    fun mean(): Double = values.sumOf { it } / size

    fun standardDeviation(): Double {
        // two-pass algorithm for variance, avoids numeric overflow
        if (size <= 1)
            return 0.0

        val mean = mean()
        val sum = values.sumOf { (it - mean).let { it * it } }
        val variance = sum / values.lastIndex
        return sqrt(variance)
    }

    @KotlinxBenchmarkRuntimeInternalApi
    companion object {
        fun createResult(
            benchmark: BenchmarkDescriptor<*>,
            params: Map<String, String>,
            config: BenchmarkConfiguration,
            samples: DoubleArray
        ): ReportBenchmarkResult {
            val statistics = ReportBenchmarksStatistics(samples)
            val score = statistics.mean()
            val errorMargin = 1.96 * (statistics.standardDeviation() / sqrt(samples.size.toDouble()))
/*
            val d = (4 - log10(score).toInt()).coerceAtLeast(0) // display 4 significant digits
            val minText = statistics.min().format(d)
            val maxText = statistics.max().format(d)
            val devText = statistics.standardDeviation().format(d)
*/

            // These quantiles are inverted, because we are interested in ops/sec and the higher the better
            // so we need minimum speed at which 90% of samples run
            val percentiles = listOf(0.0, 0.25, 0.5, 0.75, 0.90, 0.99, 0.999, 0.9999, 1.0).associate {
                (it * 100) to statistics.valueAt(it)
            }
            return ReportBenchmarkResult(
                config,
                benchmark,
                params,
                score,
                errorMargin,
                (score - errorMargin) to (score + errorMargin),
                percentiles,
                samples
            )
        }
    }
}

/**
 * Pretty formats a number.
 *
 * The locale must be fixed, so that decimals will be consistently formatted.
 * - `.` - decimal separator
 * - `,` - thousands separator
 */
internal expect fun Double.format(precision: Int, useGrouping: Boolean = true): String

@KotlinxBenchmarkRuntimeInternalApi
fun Double.formatSignificant(precision: Int): String {
    val d = (precision - ceil(log10(this)).toInt()).coerceAtLeast(0) // display 4 significant digits
    return format(d)
}

private val zeroThreshold = 1.0 / 10.0.pow(3) / 2 // from jmh

@KotlinxBenchmarkRuntimeInternalApi
fun Double.isNaNOrZero(): Boolean = isNaN() || isApproximateZero()

@KotlinxBenchmarkRuntimeInternalApi
fun Double.isApproximateZero(): Boolean = this < zeroThreshold

@KotlinxBenchmarkRuntimeInternalApi
@Suppress("REDUNDANT_ELSE_IN_WHEN")
fun Double.nanosToText(mode: Mode, unit: BenchmarkTimeUnit): String {
    val value = nanosToSample(mode, unit)
    return when (mode) {
        Mode.Throughput -> "${value.formatSignificant(6)} ops/${unit.toText()}"
        Mode.AverageTime -> "${value.formatSignificant(6)} ${unit.toText()}/op"
        else -> throw UnsupportedOperationException("$mode is not supported")
    }
}

@KotlinxBenchmarkRuntimeInternalApi
@Suppress("REDUNDANT_ELSE_IN_WHEN")
fun unitText(mode: Mode, unit: BenchmarkTimeUnit) = when (mode) {
    Mode.Throughput -> "ops/${unit.toText()}"
    Mode.AverageTime -> "${unit.toText()}/op"
    else -> throw UnsupportedOperationException("$mode is not supported")
}

@KotlinxBenchmarkRuntimeInternalApi
@Suppress("REDUNDANT_ELSE_IN_WHEN")
fun Double.sampleToText(mode: Mode, unit: BenchmarkTimeUnit): String {
    val value = this
    return when (mode) {
        Mode.Throughput -> "${value.formatSignificant(6)} ops/${unit.toText()}"
        Mode.AverageTime -> "${value.formatSignificant(6)} ${unit.toText()}/op"
        else -> throw UnsupportedOperationException("$mode is not supported")
    }
}

@KotlinxBenchmarkRuntimeInternalApi
@Suppress("REDUNDANT_ELSE_IN_WHEN")
fun Double.nanosToSample(mode: Mode, unit: BenchmarkTimeUnit): Double {
    val multiplier = unit.toMultiplier() // unit in nanos
    return when (mode) {
        Mode.Throughput -> multiplier / this
        Mode.AverageTime -> this / multiplier
        else -> throw UnsupportedOperationException("$mode is not supported")
    }
}
