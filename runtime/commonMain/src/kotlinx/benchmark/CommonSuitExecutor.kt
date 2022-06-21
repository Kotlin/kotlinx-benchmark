package kotlinx.benchmark

import kotlin.time.*

abstract class CommonSuitExecutor(
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
            val cycles = warmup(benchmark, configuration, instance)
            DoubleArray(configuration.iterations) { iteration ->
                val nanosecondsPerOperation = measure(instance, benchmark, cycles)
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

    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    private val BenchmarkTimeUnit.asDurationTimeUnit: DurationUnit get() = when(this) {
        BenchmarkTimeUnit.NANOSECONDS -> DurationUnit.NANOSECONDS
        BenchmarkTimeUnit.MICROSECONDS -> DurationUnit.MICROSECONDS
        BenchmarkTimeUnit.MILLISECONDS -> DurationUnit.MILLISECONDS
        BenchmarkTimeUnit.SECONDS -> DurationUnit.SECONDS
        BenchmarkTimeUnit.MINUTES -> DurationUnit.MINUTES
        else -> TODO("Implement ${this.name}")
    }

    @OptIn(ExperimentalTime::class)
    private inline fun measure(cycles: Int, payload: () -> Unit): Double {
        var counter = cycles
        val time = TimeSource.Monotonic.measureTime {
            while (counter-- > 0) {
                payload()
            }
        }
        return time.inWholeNanoseconds.toDouble() / cycles
    }

    @OptIn(ExperimentalTime::class)
    private inline fun <T> measureWarmup(benchmark: BenchmarkDescriptor<T>, configuration: BenchmarkConfiguration, payload: () -> Unit): Int {
        var iterations = 0
        val benchmarkIterationTimeAsDuration =
            configuration.iterationTime.toDuration(configuration.iterationTimeUnit.asDurationTimeUnit)

        var currentIteration = 0

        val timeSource: TimeSource = TimeSource.Monotonic
        while(currentIteration < configuration.warmups) {
            iterations = 0
            var elapsedTime: Duration
            val startTime = timeSource.markNow()
            val maxTime = startTime + benchmarkIterationTimeAsDuration
            do {
                payload()
                elapsedTime = startTime.elapsedNow()
                iterations++
            } while (maxTime.hasNotPassedNow())

            val metricInNanos = elapsedTime.inWholeNanoseconds.toDouble() / iterations
            val sample = metricInNanos.nanosToText(configuration.mode, configuration.outputTimeUnit)
            reporter.output(executionName, benchmark.name, "Warm-up #$currentIteration: $sample")
            currentIteration++
        }
        return iterations
    }

    private fun <T> warmup(benchmark: BenchmarkDescriptor<T>, configuration: BenchmarkConfiguration, instance: T): Int = when(benchmark) {
        is BenchmarkDescriptorWithBlackholeParameter -> {
            val blackhole = benchmark.blackhole
            val delegate = benchmark.function
            measureWarmup(benchmark, configuration) {
                blackhole.consume(instance.delegate(blackhole))
            }
        }
        is BenchmarkDescriptorWithNoBlackholeParameter -> {
            val blackhole = benchmark.blackhole
            val delegate = benchmark.function
            measureWarmup(benchmark, configuration) {
                blackhole.consume(instance.delegate())
            }
        }
        else -> error("Unexpected ${benchmark::class.simpleName}")
    }

    private fun <T> measure(instance: T, benchmark: BenchmarkDescriptor<T>, cycles: Int): Double = when(benchmark) {
        is BenchmarkDescriptorWithBlackholeParameter -> {
            val blackhole = benchmark.blackhole
            val delegate = benchmark.function
            measure(cycles) {
                blackhole.consume(instance.delegate(blackhole))
            }
        }
        is BenchmarkDescriptorWithNoBlackholeParameter -> {
            val blackhole = benchmark.blackhole
            val delegate = benchmark.function
            measure(cycles) {
                blackhole.consume(instance.delegate())
            }
        }
        else -> error("Unexpected ${benchmark::class.simpleName}")
    }
}

