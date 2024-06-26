package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@KotlinxBenchmarkRuntimeInternalApi
abstract class CommonSuiteExecutor(
    executionName: String,
    configPath: String,
    xmlReporter: (() -> BenchmarkProgress)? = null
) : SuiteExecutor(executionName, configPath, xmlReporter) {

    private fun runBenchmark(
        benchmark: BenchmarkDescriptor<Any?>,
        configuration: BenchmarkConfiguration,
        parameters: Map<String, String>,
        id: String,
    ): DoubleArray?  {
        val instance = benchmark.suite.factory()
        benchmark.suite.parametrize(instance, parameters)
        benchmark.suite.setup(instance)
        var exception: Throwable? = null
        val samples = try {
            val cycles = estimateCycles(instance, benchmark, configuration)
            val measurer = createIterationMeasurer(instance, benchmark, configuration, cycles)
            warmup(id, configuration, cycles, measurer)
            measure(id, configuration, cycles, measurer)
        } catch (e: Throwable) {
            exception = e
            doubleArrayOf()
        } finally {
            benchmark.suite.teardown(instance)
            benchmark.blackhole.flush()
        }
        if (exception != null) {
            val error = exception.toString()
            val stacktrace = exception.stackTraceToString()
            reporter.endBenchmarkException(executionName, id, error, stacktrace)
            return null
        }
        return samples
    }

    private fun saveBenchmarkResults(benchmark: BenchmarkDescriptor<Any?>, configuration: BenchmarkConfiguration, parameters: Map<String, String>, id: String, samples: DoubleArray) {
        val convertedSamples = samples
            .map { it.nanosToSample(configuration.mode, configuration.outputTimeUnit) }
            .toDoubleArray()
        val result = ReportBenchmarksStatistics.createResult(benchmark, parameters, configuration, convertedSamples)
        val message = with(result) {
            "  ~ ${score.sampleToText(
                config.mode,
                config.outputTimeUnit
            )} ±${(error / score * 100).formatSignificant(2)}%"
        }
        reporter.endBenchmark(executionName, id, BenchmarkProgress.FinishStatus.Success, message)
        result(result)
    }

    override fun run(runnerConfiguration: RunnerConfiguration, benchmarks: List<BenchmarkDescriptor<Any?>>, start: () -> Unit, complete: () -> Unit) {
        start()
        for (benchmark in benchmarks) {
            val suite = benchmark.suite
            val benchmarkConfiguration = BenchmarkConfiguration(runnerConfiguration, suite)
            runWithParameters(suite.parameters, runnerConfiguration.params, suite.defaultParameters) { parameters ->
                val id = id(benchmark.name, parameters)
                reporter.startBenchmark(executionName, id)
                val samples = runBenchmark(benchmark, benchmarkConfiguration, parameters, id)
                if (samples != null) {
                    saveBenchmarkResults(benchmark, benchmarkConfiguration, parameters, id, samples)
                }
            }
        }
        complete()
    }

    private fun <T> estimateCycles(
        instance: T,
        benchmark: BenchmarkDescriptor<T>,
        configuration: BenchmarkConfiguration
    ): Int {
        val estimator = wrapBenchmarkFunction(instance, benchmark) { body ->
            var iterations = 0
            var elapsedTime = 0L
            val benchmarkIterationTime = configuration.iterationTime * configuration.iterationTimeUnit.toMultiplier()
            do {
                val subIterationDuration = measureNanoseconds(body)
                elapsedTime += subIterationDuration
                iterations++
            } while (elapsedTime < benchmarkIterationTime)
            iterations
        }
        return estimator()
    }

    private fun warmup(
        id: String,
        configuration: BenchmarkConfiguration,
        cycles: Int,
        measurer: () -> Long
    ) {
        var currentIteration = 0
        while(currentIteration < configuration.warmups) {
            val elapsedTime = measurer()
            val metricInNanos = elapsedTime.toDouble() / cycles
            val sample = metricInNanos.nanosToText(configuration.mode, configuration.outputTimeUnit)
            reporter.output(executionName, id, "Warm-up #$currentIteration: $sample")
            currentIteration++
        }
    }

    private fun measure(
        id: String,
        configuration: BenchmarkConfiguration,
        cycles: Int,
        measurer: () -> Long
    ) : DoubleArray = DoubleArray(configuration.iterations) { iteration ->
        val nanosecondsPerOperation = measurer().toDouble() / cycles
        val text = nanosecondsPerOperation.nanosToText(configuration.mode, configuration.outputTimeUnit)
        reporter.output(executionName, id, "Iteration #$iteration: $text")
        nanosecondsPerOperation
    }

    private inline fun <T, R> wrapBenchmarkFunction(
        instance: T,
        benchmark: BenchmarkDescriptor<T>,
        crossinline wrapper: (() -> Unit) -> R): () -> R = when(benchmark) {
        is BenchmarkDescriptorWithBlackholeParameter -> {
            {
                val localBlackhole = benchmark.blackhole
                val localDelegate = benchmark.function
                val localInstance = instance
                wrapper {
                    localBlackhole.consume(localInstance.localDelegate(localBlackhole))
                }
            }
        }
        is BenchmarkDescriptorWithNoBlackholeParameter -> {
            {
                val localBlackhole = benchmark.blackhole
                val localDelegate = benchmark.function
                val localInstance = instance
                wrapper {
                    localBlackhole.consume(localInstance.localDelegate())
                }
            }
        }
        else -> error("Unexpected ${benchmark::class.simpleName}")
    }

    protected open fun <T> createIterationMeasurer(
        instance: T,
        benchmark: BenchmarkDescriptor<T>,
        configuration: BenchmarkConfiguration,
        cycles: Int
    ): () -> Long = wrapBenchmarkFunction(instance, benchmark) { payload ->
        var cycle = cycles
        measureNanoseconds {
            while(cycle-- > 0) {
                payload()
            }
        }
    }
}

