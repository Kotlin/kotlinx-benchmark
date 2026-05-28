package kotlinx.benchmark

interface CommonBenchmarkExtension {
    val executionName: String

    fun runBenchmark(
        benchmark: BenchmarkDescriptor<Any?>,
        configuration: BenchmarkConfiguration,
        parameters: Map<String, String>,
        id: String,
        progress: BenchmarkProgress,
    ): DoubleArray?  {
        val instance = benchmark.suite.factory()
        benchmark.suite.parametrize(instance, parameters)
        benchmark.suite.setup(instance)
        var exception: Throwable? = null
        val samples = try {
            val cycles = estimateCycles(instance, benchmark, configuration)
            val measurer = createIterationMeasurer(instance, benchmark, configuration, cycles)
            warmup(id, configuration, cycles, measurer, progress)
            measure(id, configuration, cycles, measurer, progress)
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
            progress.endBenchmarkException(executionName, id, error, stacktrace)
            return null
        }
        return samples
    }

    private fun <T> estimateCycles(
        instance: T,
        benchmark: BenchmarkDescriptor<T>,
        configuration: BenchmarkConfiguration,
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
        measurer: () -> Long,
        progress: BenchmarkProgress,
    ) {
        var currentIteration = 0
        while(currentIteration < configuration.warmups) {
            val elapsedTime = measurer()
            val metricInNanos = elapsedTime.toDouble() / cycles
            val sample = metricInNanos.nanosToText(configuration.mode, configuration.outputTimeUnit)
            progress.output(executionName, id, "Warm-up #$currentIteration: $sample")
            currentIteration++
        }
    }

    private fun measure(
        id: String,
        configuration: BenchmarkConfiguration,
        cycles: Int,
        measurer: () -> Long,
        progress: BenchmarkProgress,
    ) : DoubleArray = DoubleArray(configuration.iterations) { iteration ->
        val nanosecondsPerOperation = measurer().toDouble() / cycles
        val text = nanosecondsPerOperation.nanosToText(configuration.mode, configuration.outputTimeUnit)
        progress.output(executionName, id, "Iteration #$iteration: $text")
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

    fun <T> createIterationMeasurer(
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

internal inline fun measureNanoseconds(block: () -> Unit): Long {
    val measurer =  engineSupport.getMeasurer()
    measurer.measureStart()
    block()
    return measurer.measureFinish()
}