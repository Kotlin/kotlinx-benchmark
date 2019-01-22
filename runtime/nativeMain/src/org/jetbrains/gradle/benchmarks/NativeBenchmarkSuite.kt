package org.jetbrains.gradle.benchmarks.native

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
            ReportBenchmarksStatistics.createResult(benchmark.name, samples)
        }
        println()

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

