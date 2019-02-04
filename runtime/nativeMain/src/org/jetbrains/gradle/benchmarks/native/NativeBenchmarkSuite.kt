package org.jetbrains.gradle.benchmarks.native

import org.jetbrains.gradle.benchmarks.*
import platform.posix.*
import kotlin.system.*

class Suite(val title: String, private val args: Array<out String>) {
    private class BenchmarkDescriptor(
        val name: String,
        val function: () -> Any?,
        val setup: () -> Unit,
        val teardown: () -> Unit
    )

    private val benchmarks = mutableListOf<BenchmarkDescriptor>()
    private val reportFile = args[0]
    private val iterations = args[1].toInt()
    private val iterationTime = args[2].toInt()
    private val format = args[3]

    fun add(name: String, function: () -> Any?, setup: () -> Unit, teardown: () -> Unit) {
        benchmarks.add(BenchmarkDescriptor(name, function, setup, teardown))
    }

    fun run() {
        when (format) {
            "xml" -> {
                println(ijLogStart(title, ""))
            }
            "text" -> {}
            else -> throw UnsupportedOperationException("Format $format is not supported.")
        }

        val results = benchmarks.map { benchmark ->
            val benchmarkFQN = benchmark.name
            when (format) {
                "xml" -> {
                    println(ijLogStart(benchmarkFQN, title))
                }
                "text" -> {
                    println()
                    println("… $benchmarkFQN")
                }
                else -> throw UnsupportedOperationException("Format $format is not supported.")
            }
            
            benchmark.setup()
            val samples = try {
                // Execute warmup
                val cycles = warmup(benchmark)
                DoubleArray(iterations) {
                    measure(benchmark, cycles)
                }
            } finally {
                benchmark.teardown()
            }
            val result = ReportBenchmarksStatistics.createResult(benchmarkFQN, samples)
            val message = with(result) {
                val d = (4 - kotlin.math.log10(score).toInt()).coerceAtLeast(0) // display 4 significant digits
                "  ~ ${score.format(d)} ops/sec ±${(error / score * 100).format(2)}%"
            }

            when (format) {
                "xml" -> {
                    println(ijLogFinish(benchmarkFQN, title))
                    println(ijLogOutput(benchmarkFQN, title, message))
                }
                "text" -> {
                    println("  $message")
                }
                else -> throw UnsupportedOperationException("Format $format is not supported.")
            }
            result
        }
        when (format) {
            "xml" -> {
                println(ijLogFinish(title, ""))
            }
            "text" -> {
                println()
            }
            else -> throw UnsupportedOperationException("Format $format is not supported.")
        }

        val file = fopen(reportFile, "w")
        fputs(results.toJson(), file)
        fclose(file)
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

