package org.jetbrains.gradle.benchmarks.native

import kotlinx.cli.*
import org.jetbrains.gradle.benchmarks.*
import platform.posix.*
import kotlin.system.*

class Suite(private val title: String, args: Array<out String>) {
    private val params = RunnerCommandLine().also { it.parse(args) }

    private class BenchmarkDescriptor(
        val name: String,
        val function: () -> Any?,
        val setup: () -> Unit,
        val teardown: () -> Unit
    )

    private val benchmarks = mutableListOf<BenchmarkDescriptor>()

    fun add(name: String, function: () -> Any?, setup: () -> Unit, teardown: () -> Unit) {
        benchmarks.add(BenchmarkDescriptor(name, function, setup, teardown))
    }

    fun run() {
        val reportFile = params.reportFile ?: run {
            println("Report file should be specified")
            return
        }

        when (params.traceFormat) {
            "xml" -> {
                println(ijLogStart(title, ""))
            }
            "text" -> {}
            else -> throw UnsupportedOperationException("Format ${params.traceFormat} is not supported.")
        }

        val results = benchmarks.map { benchmark ->
            val benchmarkFQN = benchmark.name
            when (params.traceFormat) {
                "xml" -> {
                    println(ijLogStart(benchmarkFQN, title))
                }
                "text" -> {
                    println()
                    println("… $benchmarkFQN")
                }
                else -> throw UnsupportedOperationException("Format ${params.traceFormat} is not supported.")
            }
            
            benchmark.setup()
            val samples = try {
                // Execute warmup
                val cycles = warmup(benchmark)
                DoubleArray(params.iterations) {
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

            when (params.traceFormat) {
                "xml" -> {
                    println(ijLogFinish(benchmarkFQN, title))
                    println(ijLogOutput(benchmarkFQN, title, message))
                }
                "text" -> {
                    println("  $message")
                }
                else -> throw UnsupportedOperationException("Format ${params.traceFormat} is not supported.")
            }
            result
        }
        when (params.traceFormat) {
            "xml" -> {
                println(ijLogFinish(title, ""))
            }
            "text" -> {
                println()
            }
            else -> throw UnsupportedOperationException("Format ${params.traceFormat} is not supported.")
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
        val benchmarkNanos = params.iterationTime * 1_000_000
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

