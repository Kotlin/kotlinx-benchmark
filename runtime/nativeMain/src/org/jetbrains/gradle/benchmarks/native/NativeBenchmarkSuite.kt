package org.jetbrains.gradle.benchmarks.native

import kotlinx.cli.*
import org.jetbrains.gradle.benchmarks.*
import kotlin.system.*

class Suite(private val suiteName: String, args: Array<out String>) {
    private val params = RunnerCommandLine().also { it.parse(args) }
    private val reporter = BenchmarkReporter.create(params.reportFile, params.traceFormat)
    private val filter = params.filter

    private class BenchmarkDescriptor(
        val name: String,
        val function: () -> Any?,
        val setup: () -> Unit,
        val teardown: () -> Unit
    )

    private val benchmarks = mutableListOf<BenchmarkDescriptor>()

    fun add(name: String, function: () -> Any?, setup: () -> Unit, teardown: () -> Unit) {
        if (!matchesFilter(name))
            return
        benchmarks.add(BenchmarkDescriptor(name, function, setup, teardown))
    }

    private fun matchesFilter(name: String): Boolean {
        return filter == null || name.indexOf(filter) != -1
    }

    fun run() {
        reporter.startSuite(suiteName)

        val results = benchmarks.map { benchmark ->
            val benchmarkName = benchmark.name
            reporter.startBenchmark(suiteName, benchmarkName)

            benchmark.setup()
            var exception : Throwable? = null
            val samples = try {
                // Execute warmup
                val cycles = warmup(benchmark)
                DoubleArray(params.iterations) {
                    measure(benchmark, cycles)
                }
            } catch (e: Throwable) {
                exception = e
                doubleArrayOf()
            } finally {
                benchmark.teardown()
            }
            
            if (exception == null) {
                val result = ReportBenchmarksStatistics.createResult(benchmarkName, samples)
                val message = with(result) {
                    val d = (4 - kotlin.math.log10(score).toInt()).coerceAtLeast(0) // display 4 significant digits
                    "  ~ ${score.format(d)} ops/sec ±${(error / score * 100).format(2)}%"
                }

                reporter.endBenchmark(suiteName, benchmarkName, BenchmarkReporter.FinishStatus.Success, message)
                result
            } else {
                val error = exception.toString()
                val stacktrace = exception.stacktrace()
                reporter.endBenchmarkException(suiteName, benchmarkName, error, stacktrace)
                ReportBenchmarksStatistics.createResult(benchmarkName, samples)
            }
        }

        reporter.endSuite(suiteName, results)
    }
    
    private fun Throwable.stacktrace() : String {
        val nested = cause ?: return getStackTrace().joinToString("\n")
        return getStackTrace().joinToString("\n") + "\nCause: ${nested.message}\n" + nested.stacktrace()
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

