package org.jetbrains.gradle.benchmarks.native

import kotlinx.cinterop.*
import org.jetbrains.gradle.benchmarks.*
import platform.posix.*
import kotlin.system.*

class Suite(private val args: Array<out String>) {
    private class BenchmarkDescriptor(
        val name: String,
        val function: () -> Any?,
        val setup: () -> Unit,
        val teardown: () -> Unit
    )

    private val benchmarks = mutableListOf<BenchmarkDescriptor>()
    private val reportFile = args.first()

    val iterationTime = 1000 // ms
    val iterationNumber = 10 // times

    fun add(name: String, function: () -> Any?, setup: () -> Unit, teardown: () -> Unit) {
        benchmarks.add(BenchmarkDescriptor(name, function, setup, teardown))
    }

    fun run() {
        val results = benchmarks.map { benchmark ->
            println()
            println("… ${benchmark.name}")

            benchmark.setup()
            val samples = try {
                // Execute warmup
                val cycles = warmup(benchmark)
                DoubleArray(iterationNumber) {
                    measure(benchmark, cycles)
                }
            } finally {
                benchmark.teardown()
            }
            val statistics = NativeBenchmarksStatistics(samples)
            val score = statistics.median()
            val d = (4 - log10(score).toInt()).coerceAtLeast(0) // display 4 significant digits

            println("  ~ ${score.format(d)} ops/sec")
            val minText = statistics.min().format(d)
            val meanText = statistics.mean().format(d)
            val maxText = statistics.max().format(d)
            val devText = statistics.standardDeviation().format(d)
            println("    min: $minText, avg: $meanText, max: $maxText, stddev: $devText")

            // These quantiles are inverted, because we are interested in ops/sec and the higher the better
            // so we need minimum speed at which 90% of samples run
            val p90 = statistics.valueAt(0.1).format(d)
            val p75 = statistics.valueAt(0.25).format(d)
            val p50 = statistics.valueAt(0.5).format(d)
            println("    90%: $p90, 75%: $p75, 50%: $p50")
            val moe = 1.96 * statistics.standardDeviation()
            val mean = statistics.mean()
            ReportBenchmarkResult(benchmark.name, score, (mean - moe) to (mean + moe), samples)
        }
        println()

        val file = fopen(reportFile, "w")
        fputs(results.toJson(), file)
        fclose(file)
    }

    private fun Double.format(precision: Int): String = memScoped {
        val bytes = allocArray<ByteVar>(100)
        sprintf(bytes, "%.${precision}F", this@format)
        return bytes.toKString()
    }

    private fun measure(benchmark: BenchmarkDescriptor, cycles: Int): Double {
        val executeFunction = benchmark.function
        var counter = cycles
        val startTime = getTimeNanos()
        while (counter-- > 0) {
            @Suppress("UNUSED_VARIABLE")
            val result = executeFunction() // ignore result for now, but might need to consume it somehow
        }
        val endTime = getTimeNanos()
        val time = endTime - startTime
        val nanosecondsPerOperation = time.toDouble() / cycles
        val operationsPerSecond = 1_000_000_000.0 / nanosecondsPerOperation
        return operationsPerSecond
    }

    private fun warmup(benchmark: BenchmarkDescriptor): Int {
        val benchmarkNanos = iterationTime * 1_000_000
        val startTime = getTimeNanos()
        var endTime = startTime
        var iterations = 0
        val executeFunction = benchmark.function
        while (endTime - startTime < benchmarkNanos) {
            executeFunction()
            endTime = getTimeNanos()
            iterations++
        }
        return iterations
    }
}

