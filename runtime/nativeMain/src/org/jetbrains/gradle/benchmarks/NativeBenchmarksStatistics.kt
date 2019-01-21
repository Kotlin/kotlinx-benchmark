package org.jetbrains.gradle.benchmarks.native

import kotlin.math.*

class NativeBenchmarksStatistics(values: DoubleArray) {
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

    fun mean(): Double = values.sumByDouble { it } / size

    fun standardDeviation(): Double {
        // two-pass algorithm for variance, avoids numeric overflow
        if (size <= 1)
            return 0.0

        val mean = mean()
        val sum = values.sumByDouble { (it - mean).let { it * it } }
        val variance = sum / values.lastIndex
        return sqrt(variance)
    }
}