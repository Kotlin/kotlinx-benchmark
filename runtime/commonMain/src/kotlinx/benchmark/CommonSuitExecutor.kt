package kotlinx.benchmark

abstract class CommonSuitExecutor(
    executionName: String,
    configPath: String,
    private val blackHole: Blackhole? = null,
    xmlReporter: (() -> BenchmarkProgress)? = null
) : SuiteExecutor(executionName, configPath, xmlReporter) {

    abstract fun getTimeNanos(): Long

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
            val cycles = warmup(benchmark, configuration, instance)
            DoubleArray(benchmark.suite.iterations) { iteration ->
                val nanosecondsPerOperation = measure(instance, benchmark, cycles)
                val text = nanosecondsPerOperation.nanosToText(configuration.mode, configuration.outputTimeUnit)
                reporter.output(executionName, id, "Iteration #$iteration: $text")
                nanosecondsPerOperation.nanosToSample(configuration.mode, configuration.outputTimeUnit)
            }
        } catch (e: Throwable) {
            exception = e
            doubleArrayOf()
        } finally {
            benchmark.suite.teardown(instance)
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
            .map {
                val nanos = it * BenchmarkTimeUnit.SECONDS.toMultiplier()
                nanos.nanosToSample(configuration.mode, configuration.outputTimeUnit)
            }
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

    override fun run(
        runnerConfiguration: RunnerConfiguration,
        benchmarks: List<BenchmarkDescriptor<Any?>>,
        start: () -> Unit,
        complete: () -> Unit
    ) {
        start()
        for (benchmark in benchmarks) {
            val suite = benchmark.suite
            val benchmarkConfiguration = BenchmarkConfiguration(runnerConfiguration, benchmark.suite)
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

    private fun <T> measure(instance: T, benchmark: BenchmarkDescriptor<T>, cycles: Int): Double {
        val executeFunction = benchmark.function
        var counter = cycles
        val blackHole = blackHole
        val startTime = getTimeNanos()
        while (counter-- > 0) {
            val result = instance.executeFunction()
            blackHole?.consume(result)
        }
        val endTime = getTimeNanos()
        val time = endTime - startTime
        return time.toDouble() / cycles
    }

    private fun <T> warmup(benchmark: BenchmarkDescriptor<T>, configuration: BenchmarkConfiguration, instance: T): Int {
        var iterations = 0
        repeat(configuration.warmups) { iteration ->
            val benchmarkNanos = configuration.iterationTime * configuration.iterationTimeUnit.toMultiplier()
            val executeFunction = benchmark.function

            val startTime = getTimeNanos()
            var endTime = startTime
            while (endTime - startTime < benchmarkNanos) {
                val result = instance.executeFunction()
                blackHole?.consume(result)
                endTime = getTimeNanos()
                iterations++
            }
            val time = endTime - startTime
            val metric = time.toDouble() / iterations
            val sample = metric.nanosToText(configuration.mode, configuration.outputTimeUnit)
            reporter.output(benchmark.suite.name, benchmark.name, "Warm-up #$iteration: $sample")
        }
        return iterations
    }
}

