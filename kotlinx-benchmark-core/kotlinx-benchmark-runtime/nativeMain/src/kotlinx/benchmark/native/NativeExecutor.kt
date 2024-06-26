package kotlinx.benchmark.native

import kotlinx.benchmark.*
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import kotlin.native.runtime.GC
import kotlin.time.*

@KotlinxBenchmarkRuntimeInternalApi
class NativeExecutor(
    name: String,
    args: Array<out String>
) : SuiteExecutor(name, args[0], { NativeIntelliJBenchmarkProgress(args[2]) }) {

    private val action = args[1]
    private val additionalArguments = args.drop(3)

    data class BenchmarkRun(val benchmarkName: String, val config: BenchmarkConfiguration,
                            val parameters: Map<String, String>)

    private val BenchmarkConfiguration.nativeFork: NativeFork
        get() = advanced["nativeFork"]
            ?.let { NativeFork.values()
            .firstOrNull { entity -> entity.name.equals(it, ignoreCase = true) } }
            ?: NativeFork.PerBenchmark

    private val BenchmarkConfiguration.nativeGCAfterIteration: Boolean
        get() = "true".equals(advanced["nativeGCAfterIteration"], ignoreCase = true)

    private fun outputBenchmarks(runnerConfiguration: RunnerConfiguration,
                                 benchmarks: List<BenchmarkDescriptor<Any?>>,
                                 start: () -> Unit) {
        start()
        benchmarks.forEach {
            val suite = it.suite
            val config = BenchmarkConfiguration(runnerConfiguration, suite)
            var parametersId = 0
            runWithParameters(suite.parameters, runnerConfiguration.params, suite.defaultParameters) { params ->
                val benchmarkRunConfig = buildString {
                    appendLine("benchmark: ${it.name}")
                    appendLine("configuration: $config")
                    appendLine("parameters: $params")
                }
                val fileName = "${additionalArguments[0]}/${suite.name}_${it.name}_$parametersId.txt"
                fileName.writeFile(benchmarkRunConfig)
                parametersId++
            }
        }
    }

    private fun List<BenchmarkDescriptor<Any?>>.getBenchmark(name: String) = find { it.name == name }
        ?: throw NoSuchElementException("Benchmark $name wasn't found.")

    private fun runBenchmarkIteration(benchmarks: List<BenchmarkDescriptor<Any?>>) {
        val (configFileName, iteration, cycles, resultsFile) = additionalArguments
        val benchmarkRun = configFileName.parseBenchmarkConfig()
        val benchmark = benchmarks.getBenchmark(benchmarkRun.benchmarkName)
        val samples = run(benchmark, benchmarkRun, iteration.toInt(), cycles.toInt())
        resultsFile.writeFile(samples?.let{ it[0].toString() } ?: "null")
    }

    private fun runBenchmarkWarmup(benchmarks: List<BenchmarkDescriptor<Any?>>) {
        val (configFileName, iteration, resultsFile) = additionalArguments
        val benchmarkRun = configFileName.parseBenchmarkConfig()
        val benchmark = benchmarks.getBenchmark(benchmarkRun.benchmarkName)
        val id = id(benchmark.name, benchmarkRun.parameters)

        val instance = benchmark.suite.factory() // TODO: should we create instance per bench or per suite?
        benchmark.suite.parametrize(instance, benchmarkRun.parameters)
        benchmark.suite.setup(instance)

        if (iteration.toInt() == 0) {
            reporter.startBenchmark(executionName, id)
        }
        try {
            val iterations = warmup(benchmark.suite.name, benchmarkRun.config, instance,
                benchmark, iteration.toInt())
            resultsFile.writeFile(iterations.toString())
        } catch (e: Throwable) {
            val error = e.toString()
            val stacktrace = e.stacktrace()
            reporter.endBenchmarkException(executionName, id, error, stacktrace)
            resultsFile.writeFile("null")
        } finally {
            benchmark.suite.teardown(instance)
            benchmark.blackhole.flush()
        }
    }

    private fun runBenchmark(benchmarks: List<BenchmarkDescriptor<Any?>>) {
        val (configFileName, resultsFile) = additionalArguments
        val benchmarkRun = configFileName.parseBenchmarkConfig()
        val benchmark = benchmarks.getBenchmark(benchmarkRun.benchmarkName)
        val id = id(benchmark.name, benchmarkRun.parameters)
        reporter.startBenchmark(executionName, id)
        val samples = run(benchmark, benchmarkRun)
        if (samples != null) {
            resultsFile.writeFile(samples.joinToString())
            saveBenchmarkResults(benchmark, benchmarkRun, samples)
        }
    }

    private fun endForkedIterationsRun(benchmarks: List<BenchmarkDescriptor<Any?>>) {
        val (configFileName, samplesFile) = additionalArguments
        val samples = samplesFile.readFile().split(", ").map { it.toDouble() }.toDoubleArray()
        val benchmarkRun = configFileName.parseBenchmarkConfig()
        val benchmark = benchmarks.getBenchmark(benchmarkRun.benchmarkName)
        saveBenchmarkResults(benchmark, benchmarkRun, samples)
    }

    fun run(
        benchmark: BenchmarkDescriptor<Any?>,
        benchmarkRun: BenchmarkRun,
        currentIteration: Int? = null,
        cyclesPerIteration: Int? = null
    ): DoubleArray?  {

        require((currentIteration == null) == (cyclesPerIteration == null)) {
            "Current iteration number must be provided if and only if the number of cycles per iteration is provided"
        }
        require(benchmarkRun.config.nativeFork == NativeFork.PerIteration && currentIteration != null
                || benchmarkRun.config.nativeFork == NativeFork.PerBenchmark && currentIteration == null) {
            "Fork must be per benchmark or current iteration number must be provided, but not both at the same time"
        }

        val id = id(benchmark.name, benchmarkRun.parameters)
        val suite = benchmark.suite

        val instance = suite.factory() // TODO: should we create instance per bench or per suite?
        suite.parametrize(instance, benchmarkRun.parameters)
        benchmark.suite.setup(instance)

        var exception: Throwable? = null
        val iterations = if (benchmarkRun.config.nativeFork == NativeFork.PerIteration) 1 else benchmarkRun.config.iterations
        val samples = try {
            // Execute warmup
            val cycles = cyclesPerIteration ?: warmup(suite.name, benchmarkRun.config, instance, benchmark)
            DoubleArray(iterations) { iteration ->
                val nanosecondsPerOperation = measure(instance, benchmark, cycles, benchmarkRun.config.nativeGCAfterIteration)
                val text = nanosecondsPerOperation.nanosToText(benchmarkRun.config.mode, benchmarkRun.config.outputTimeUnit)
                val iterationNumber = currentIteration ?: iteration
                reporter.output(
                    executionName,
                    id,
                    "Iteration #$iterationNumber: $text"
                )
                nanosecondsPerOperation.nanosToSample(benchmarkRun.config.mode, benchmarkRun.config.outputTimeUnit)
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
            val stacktrace = exception.stacktrace()
            reporter.endBenchmarkException(executionName, id, error, stacktrace)
            return null
        }
        return samples
    }

    private fun saveBenchmarkResults(benchmark: BenchmarkDescriptor<Any?>, benchmarkRun: BenchmarkRun,
                              samples: DoubleArray) {
        val id = id(benchmark.name, benchmarkRun.parameters)
        val result = ReportBenchmarksStatistics.createResult(benchmark, benchmarkRun.parameters, benchmarkRun.config, samples)
        val message = with(result) {
            // TODO: metric
            "  ~ ${
                score.sampleToText(
                    benchmarkRun.config.mode,
                    benchmarkRun.config.outputTimeUnit
                )
            } ±${(error / score * 100).formatSignificant(2)}%"
        }

        reporter.endBenchmark(
            executionName,
            id,
            BenchmarkProgress.FinishStatus.Success,
            message
        )
        result(result)
    }

    private fun storeResults(benchmarks: List<BenchmarkDescriptor<Any?>>, complete: () -> Unit) {
        val resultsContent = additionalArguments[0].readFile()
        resultsContent.takeIf(String::isNotEmpty)?.lines()?.forEach {
            val (configFileName, samplesList) = it.split(": ")
            val samples = samplesList.split(", ").map { it.toDouble() }.toDoubleArray()
            val benchmarkRun = configFileName.parseBenchmarkConfig()
            val benchmark = benchmarks.getBenchmark(benchmarkRun.benchmarkName)
            val result = ReportBenchmarksStatistics.createResult(benchmark, benchmarkRun.parameters,
                benchmarkRun.config, samples)
            result(result)
        }
        complete()
    }

    override fun run(
        runnerConfiguration: RunnerConfiguration,
        benchmarks: List<BenchmarkDescriptor<Any?>>,
        start: () -> Unit,
        complete: () -> Unit
    ) {
        when (action) {
            "--list" -> outputBenchmarks(runnerConfiguration, benchmarks, start)
            "--store-results" -> storeResults(benchmarks, complete)
            "--benchmark" -> runBenchmark(benchmarks)
            "--iteration" -> runBenchmarkIteration(benchmarks)
            "--warmup" -> runBenchmarkWarmup(benchmarks)
            "--end-run" -> endForkedIterationsRun(benchmarks)
            else -> throw IllegalArgumentException("Unknown action: $action.")
        }
    }
    
    private fun Throwable.stacktrace(): String {
        val nested = cause ?: return getStackTrace().joinToString("\n")
        return getStackTrace().joinToString("\n") + "\nCause: ${nested.message}\n" + nested.stacktrace()
    }

    private inline fun measure(
        cycles: Int,
        nativeGCAfterIteration: Boolean,
        body: () -> Unit,
    ): Double {
        if (nativeGCAfterIteration)
            GC.collect()

        val duration = measureTime {
            var counter = cycles
            while (counter-- > 0) {
                body()
            }
            if (nativeGCAfterIteration)
                GC.collect()
        }

        return duration.toDouble(DurationUnit.NANOSECONDS) / cycles
    }

    private inline fun <T> measureWarmup(
        name: String,
        config: BenchmarkConfiguration,
        benchmark: BenchmarkDescriptor<T>,
        currentIteration: Int?,
        body: () -> Unit
    ): Int {
        require(config.nativeFork == NativeFork.PerIteration && currentIteration != null
                || config.nativeFork == NativeFork.PerBenchmark && currentIteration == null) {
            "Fork must be per benchmark or current iteration number must be provided, but not both at the same time"
        }

        var iterations = 0
        val warmupIterations = if (config.nativeFork == NativeFork.PerIteration) 1 else config.warmups
        repeat(warmupIterations) { iteration ->
            val benchmarkNanos = config.iterationTime * config.iterationTimeUnit.toMultiplier()

            if (config.nativeGCAfterIteration)
                GC.collect()
            val startTime = TimeSource.Monotonic.markNow()
            var duration = Duration.ZERO
            iterations = 0
            while (duration.inWholeNanoseconds < benchmarkNanos) {
                body()
                duration = startTime.elapsedNow()
                iterations++
            }
            if (config.nativeGCAfterIteration)
                GC.collect()

            val metric = duration.toDouble(DurationUnit.NANOSECONDS) / iterations // TODO: metric
            val sample = metric.nanosToText(config.mode, config.outputTimeUnit)
            val iterationNumber = currentIteration ?: iteration
            reporter.output(name, benchmark.name, "Warm-up #$iterationNumber: $sample")
        }
        return iterations
    }

    private fun <T> warmup(
        name: String,
        config: BenchmarkConfiguration,
        instance: T,
        benchmark: BenchmarkDescriptor<T>,
        currentIteration: Int? = null
    ): Int = when(benchmark) {
        is BenchmarkDescriptorWithBlackholeParameter -> {
            val blackhole = benchmark.blackhole
            val delegate = benchmark.function
            measureWarmup(name, config, benchmark, currentIteration) {
                blackhole.consume(instance.delegate(blackhole))
            }
        }
        is BenchmarkDescriptorWithNoBlackholeParameter -> {
            val blackhole = benchmark.blackhole
            val delegate = benchmark.function
            measureWarmup(name, config, benchmark, currentIteration) {
                blackhole.consume(instance.delegate())
            }
        }
        else -> error("Unexpected ${benchmark::class.simpleName}")
    }

    private fun <T> measure(
        instance: T,
        benchmark: BenchmarkDescriptor<T>,
        cycles: Int,
        nativeGCAfterIteration: Boolean,
    ): Double = when(benchmark) {
        is BenchmarkDescriptorWithBlackholeParameter -> {
            val blackhole = benchmark.blackhole
            val delegate = benchmark.function
            measure(cycles, nativeGCAfterIteration) {
                blackhole.consume(instance.delegate(blackhole))
            }
        }
        is BenchmarkDescriptorWithNoBlackholeParameter -> {
            val blackhole = benchmark.blackhole
            val delegate = benchmark.function
            measure(cycles, nativeGCAfterIteration) {
                blackhole.consume(instance.delegate())
            }
        }
        else -> error("Unexpected ${benchmark::class.simpleName}")
    }
}

