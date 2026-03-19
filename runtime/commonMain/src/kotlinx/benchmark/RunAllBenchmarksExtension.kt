package kotlinx.benchmark

/**
 * Runs a complete benchmark suite by iterating over all configured benchmarks and their parameter combinations.
 *
 * Implementations are responsible for:
 * - reporting benchmark start and end events through [reporter]
 * - executing a single benchmark in [runBenchmark]
 * - publishing the computed result in [result]
 *
 * Contract:
 * - [run] must call [start] before executing the first benchmark and [complete] after all benchmarks finish
 * - [runBenchmark] should return raw sample values in nanoseconds, or `null` if the benchmark produced no result
 * - [saveBenchmarkResults] converts raw samples, records the formatted result, and notifies [reporter]
 */
internal interface RunAllBenchmarksExtension {
    val reporter: BenchmarkProgress
    val executionName: String
    fun result(result: ReportBenchmarkResult)

    fun runBenchmark(
        benchmark: BenchmarkDescriptor<Any?>,
        configuration: BenchmarkConfiguration,
        parameters: Map<String, String>,
        id: String,
        progress: BenchmarkProgress,
    ): DoubleArray?

    fun saveBenchmarkResults(
        benchmark: BenchmarkDescriptor<Any?>,
        configuration: BenchmarkConfiguration,
        parameters: Map<String, String>,
        id: String,
        samples: DoubleArray
    ) {
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

    fun run(runnerConfiguration: RunnerConfiguration, benchmarks: List<BenchmarkDescriptor<Any?>>, start: () -> Unit, complete: () -> Unit) {
        start()
        for (benchmark in benchmarks) {
            val suite = benchmark.suite
            val benchmarkConfiguration = BenchmarkConfiguration(runnerConfiguration, suite)
            runWithParameters(suite.parameters, runnerConfiguration.params, suite.defaultParameters) { parameters ->
                val id = id(benchmark.name, parameters)
                reporter.startBenchmark(executionName, id)
                val samples = runBenchmark(benchmark, benchmarkConfiguration, parameters, id, reporter)
                if (samples != null) {
                    saveBenchmarkResults(benchmark, benchmarkConfiguration, parameters, id, samples)
                }
            }
        }
        complete()
    }
}