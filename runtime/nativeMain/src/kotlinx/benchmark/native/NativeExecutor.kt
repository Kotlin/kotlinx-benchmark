package kotlinx.benchmark.native

import kotlinx.benchmark.*
import kotlin.system.*

class NativeExecutor(name: String, args: Array<out String>) : SuiteExecutor(name, args) {
    override fun run(
        runnerConfiguration: RunnerConfiguration,
        reporter: BenchmarkProgress,
        benchmarks: List<BenchmarkDescriptor<Any?>>,
        complete: () -> Unit
    ) {
        benchmarks.forEach { benchmark ->
            val suite = benchmark.suite
            val config = BenchmarkConfiguration(runnerConfiguration, suite)

            runWithParameters(suite.parameters, runnerConfiguration.params, suite.defaultParameters) { params ->
                val id = id(benchmark.name, params)

                val instance = suite.factory() // TODO: should we create instance per bench or per suite?
                suite.parametrize(instance, params)
                benchmark.suite.setup(instance)

                reporter.startBenchmark(executionName, id)
                var exception: Throwable? = null
                val samples = try {
                    // Execute warmup
                    val cycles = warmup(suite.name, config, instance, benchmark)
                    DoubleArray(config.iterations) { iteration ->
                        val nanosecondsPerOperation = measure(instance, benchmark, cycles)
                        val text = nanosecondsPerOperation.nanosToText(config.mode, config.outputTimeUnit)
                        reporter.output(
                            executionName,
                            id,
                            "Iteration #$iteration: $text"
                        )
                        nanosecondsPerOperation.nanosToSample(config.mode, config.outputTimeUnit)
                    }
                } catch (e: Throwable) {
                    exception = e
                    doubleArrayOf()
                } finally {
                    benchmark.suite.teardown(instance)
                }

                if (exception == null) {
                    val result = ReportBenchmarksStatistics.createResult(benchmark, params, config, samples)
                    val message = with(result) {
                        // TODO: metric
                        "  ~ ${score.sampleToText(
                            config.mode,
                            config.outputTimeUnit
                        )} ±${(error / score * 100).formatSignificant(2)}%"
                    }

                    reporter.endBenchmark(
                        executionName,
                        id,
                        BenchmarkProgress.FinishStatus.Success,
                        message
                    )
                    result(result)
                } else {
                    val error = exception.toString()
                    val stacktrace = exception.stacktrace()
                    reporter.endBenchmarkException(executionName, id, error, stacktrace)
                }
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

    private fun <T> warmup(
        name: String,
        config: BenchmarkConfiguration,
        instance: T,
        benchmark: BenchmarkDescriptor<T>
    ): Int {
        var iterations = 0
        repeat(config.warmups) { iteration ->
            val benchmarkNanos = config.iterationTime * config.iterationTimeUnit.toMultiplier()
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
            val sample = metric.nanosToText(config.mode, config.outputTimeUnit)
            reporter.output(name, benchmark.name, "Warm-up #$iteration: $sample")
        }
        return iterations
    }
}

