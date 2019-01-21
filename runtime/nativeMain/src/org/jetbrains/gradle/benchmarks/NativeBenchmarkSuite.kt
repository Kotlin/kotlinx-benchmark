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

    var iterationTime = 1000 // ms

    fun add(name: String, function: () -> Any?, setup: () -> Unit, teardown: () -> Unit) {
        benchmarks.add(BenchmarkDescriptor(name, function, setup, teardown))
    }

    fun run() {
        val results = benchmarks.map { benchmark ->
            println()
            println("… ${benchmark.name}")

            benchmark.setup()
            val operationsPerSecond = try {
                // Execute warmup
                val iterations = warmup(benchmark)
                measure(benchmark, iterations)
            } finally {
                benchmark.teardown()
            }
            println("  ~ $operationsPerSecond ops/sec")
            ReportBenchmarkResult(benchmark.name, operationsPerSecond)
        }
        println()

        val file = fopen(reportFile, "w")
        fputs(results.toJson(), file)
        fclose(file)
    }

    private fun measure(benchmark: BenchmarkDescriptor, iterations: Int): Double {
        val executeFunction = benchmark.function
        var counter = iterations
        val startTime = getTimeNanos()
        while (counter-- > 0) {
            @Suppress("UNUSED_VARIABLE")
            val result = executeFunction() // ignore result for now, but might need to consume it somehow
        }
        val endTime = getTimeNanos()
        val time = endTime - startTime
        val nanosecondsPerOperation = time.toDouble() / iterations
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

