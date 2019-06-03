package org.jetbrains.gradle.benchmarks.native

import org.jetbrains.gradle.benchmarks.*
import kotlin.system.*

class NativeExecutor(name: String, args: Array<out String>) : SuiteExecutor(name, args) {

    override fun run(reporter: BenchmarkReporter, benchmarks: List<BenchmarkDescriptor<Any?>>, complete: () -> Unit) {
        benchmarks.forEach { benchmark ->
            val suite = benchmark.suite
            reporter.startBenchmark(executionName, benchmark.name)

            val instance = suite.factory() // TODO: should we create instance per bench or per suite?
            benchmark.suite.setup(instance)
            var exception: Throwable? = null
            val samples = try {
                // Execute warmup
                val cycles = warmup(suite, instance, benchmark)
                DoubleArray(suite.iterations) { iteration->
                    val nanosecondsPerOperation = measure(instance, benchmark, cycles)
                    val text = nanosecondsPerOperation.nanosToText(suite.mode, suite.outputUnit)
                    reporter.output(
                        executionName,
                        benchmark.name,
                        "Iteration #$iteration: $text"
                    )
                    nanosecondsPerOperation.nanosToSample(suite.mode, suite.outputUnit)
                }
            } catch (e: Throwable) {
                exception = e
                doubleArrayOf()
            } finally {
                benchmark.suite.teardown(instance)
            }

            if (exception == null) {
                val result = ReportBenchmarksStatistics.createResult(benchmark.name, samples)
                val message = with(result) {
                    // TODO: metric
                    "  ~ ${score.sampleToText(suite.mode, suite.outputUnit)} ±${(error / score * 100).formatAtMost(2)}%"
                }

                reporter.endBenchmark(executionName, benchmark.name, BenchmarkReporter.FinishStatus.Success, message)
                result(result)
            } else {
                val error = exception.toString()
                val stacktrace = exception.stacktrace()
                reporter.endBenchmarkException(executionName, benchmark.name, error, stacktrace)
                result(ReportBenchmarksStatistics.createResult(benchmark.name, samples))
            }
        }
        complete()
    }

    private fun Throwable.stacktrace(): String {
        val nested = cause ?: return getStackTrace().joinToString("\n")
        return getStackTrace().joinToString("\n") + "\nCause: ${nested.message}\n" + nested.stacktrace()
    }

    private fun <T> measure(
        instance: T,
        benchmark: BenchmarkDescriptor<T>,
        cycles: Int
    ): Double {
        val executeFunction = benchmark.function
        var counter = cycles
        val startTime = getTimeNanos()
        while (counter-- > 0) {
            @Suppress("UNUSED_VARIABLE")
            val result = instance.executeFunction() // ignore result for now, but might need to consume it somehow
        }
        val endTime = getTimeNanos()
        val time = endTime - startTime
        return time.toDouble() / cycles
    }

    private fun <T> warmup(suite: SuiteDescriptor<T>, instance: T, benchmark: BenchmarkDescriptor<T>): Int {
        var iterations = 0
        repeat(suite.warmups) { iteration ->
            val benchmarkNanos = suite.iterationTime.first * suite.iterationTime.second.toMultiplier()
            val startTime = getTimeNanos()
            var endTime = startTime
            val executeFunction = benchmark.function
            iterations = 0
            while (endTime - startTime < benchmarkNanos) {
                instance.executeFunction()
                endTime = getTimeNanos()
                iterations++
            }
            val time = endTime - startTime
            val metric = time.toDouble() / iterations // TODO: metric
            val sample = metric.nanosToText(suite.mode, suite.outputUnit)
            reporter.output(suite.name, benchmark.name, "Warm-up #$iteration: $sample")
        }
        return iterations
    }
}

