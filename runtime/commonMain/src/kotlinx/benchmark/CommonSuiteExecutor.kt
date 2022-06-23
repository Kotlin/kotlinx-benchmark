package kotlinx.benchmark

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
            val measurer = createIterationMeasurer(instance, benchmark, configuration)
            val cycles = warmup(benchmark, configuration, measurer)
            DoubleArray(configuration.iterations) { iteration ->
                val nanosecondsPerOperation = measure(cycles, measurer)
                val text = nanosecondsPerOperation.nanosToText(configuration.mode, configuration.outputTimeUnit)
                reporter.output(executionName, id, "Iteration #$iteration: $text")
                nanosecondsPerOperation
            }
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

    private inline fun measure(cycles: Int, measurer: () -> Long): Double {
        var iterations = 0
        var elapsedTime = 0L
        do {
            val subIterationDuration = measurer()
            elapsedTime += subIterationDuration
            iterations++
        } while (iterations < cycles)

        return elapsedTime.toDouble() / cycles
    }

    private fun <T> warmup(benchmark: BenchmarkDescriptor<T>, configuration: BenchmarkConfiguration, measurer: () -> Long): Int {
        var iterations = 0
        val benchmarkIterationTime = configuration.iterationTime * configuration.iterationTimeUnit.toMultiplier()
        var currentIteration = 0

        while(currentIteration < configuration.warmups) {
            iterations = 0
            var elapsedTime = 0L
            do {
                val subIterationDuration = measurer()
                elapsedTime += subIterationDuration
                iterations++
            } while (elapsedTime < benchmarkIterationTime)

            val metricInNanos = elapsedTime.toDouble() / iterations
            val sample = metricInNanos.nanosToText(configuration.mode, configuration.outputTimeUnit)
            reporter.output(executionName, benchmark.name, "Warm-up #$currentIteration: $sample")
            currentIteration++
        }
        return iterations
    }

    protected open fun <T> createIterationMeasurer(instance: T, benchmark: BenchmarkDescriptor<T>, configuration: BenchmarkConfiguration): () -> Long = when(benchmark) {
        is BenchmarkDescriptorWithBlackholeParameter -> {
            {
                val localBlackhole = benchmark.blackhole
                val localDelegate = benchmark.function
                val localInstance = instance
                measureTime {
                    localBlackhole.consume(localInstance.localDelegate(localBlackhole))
                }
            }
        }
        is BenchmarkDescriptorWithNoBlackholeParameter -> {
            {
                val localBlackhole = benchmark.blackhole
                val localDelegate = benchmark.function
                val localInstance = instance
                measureTime {
                    localBlackhole.consume(localInstance.localDelegate())
                }
            }
        }
        else -> error("Unexpected ${benchmark::class.simpleName}")
    }
}

