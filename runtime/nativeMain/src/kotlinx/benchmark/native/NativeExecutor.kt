package kotlinx.benchmark.native

import kotlinx.benchmark.*
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import kotlinx.cinterop.convert
import platform.posix.usleep
import kotlin.concurrent.Volatile
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.runtime.GC
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit
import kotlin.time.measureTime

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
        val (configFileName, iteration, _, resultsFile) = additionalArguments
        val benchmarkRun = configFileName.parseBenchmarkConfig()
        val benchmark = benchmarks.getBenchmark(benchmarkRun.benchmarkName)
        val samples = run(benchmark, benchmarkRun, iteration.toInt())
        resultsFile.writeFile(samples?.let{ it[0].toString() } ?: "null")
    }

    @OptIn(ObsoleteWorkersApi::class)
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

        val threads = resolveThreadsCount(benchmarkRun.config.threads)
        WorkersPool(threads).use { workersPool ->
            try {
                warmup(
                    benchmark.suite.name, benchmarkRun.config, instance,
                    benchmark,
                    workersPool,
                    iteration.toInt()
                )
                resultsFile.writeFile("1") // Iterations number for backward compatibility
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

    @OptIn(ObsoleteWorkersApi::class)
    fun run(
        benchmark: BenchmarkDescriptor<Any?>,
        benchmarkRun: BenchmarkRun,
        currentIteration: Int? = null,
    ): DoubleArray? {
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
        val iterations =
            if (benchmarkRun.config.nativeFork == NativeFork.PerIteration) 1 else benchmarkRun.config.iterations
        val threads = resolveThreadsCount(benchmarkRun.config.threads)
        val samples = WorkersPool(threads).use { workersPool ->
             try {
                // Execute warmup
                warmup(suite.name, benchmarkRun.config, instance, benchmark, workersPool)
                val nativeGCAfterIteration = benchmarkRun.config.nativeGCAfterIteration
                DoubleArray(iterations) { iteration ->
                    val nanosecondsPerOperation =
                        measureSingleIteration(instance, benchmark,
                            (benchmarkRun.config.iterationTime * benchmarkRun.config.iterationTimeUnit.toMultiplier()).nanoseconds, nativeGCAfterIteration, workersPool)
                    val text =
                        nanosecondsPerOperation.nanosToText(
                            benchmarkRun.config.mode,
                            benchmarkRun.config.outputTimeUnit
                        )
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

    private inline fun measureSingleIterationLoop(
        synchronizer: MeasurementSynchronizer,
        nativeGCAfterIteration: Boolean,
        crossinline body: () -> Unit,
    ): Double {
        if (nativeGCAfterIteration)
            GC.collect()

        var iterations: Long = 0
        val duration = measureTime {
            while (!synchronizer.shouldStop) {
                body()
                iterations++
            }
            if (nativeGCAfterIteration)
                GC.collect()
        }

        return duration.toDouble(DurationUnit.NANOSECONDS) / iterations
    }

    private fun <T> warmup(
        name: String,
        config: BenchmarkConfiguration,
        instance: T,
        benchmark: BenchmarkDescriptor<T>,
        workers: WorkersPool,
        currentIteration: Int? = null
    ) {
        require(
            config.nativeFork == NativeFork.PerIteration && currentIteration != null
                    || config.nativeFork == NativeFork.PerBenchmark && currentIteration == null
        ) {
            "Fork must be per benchmark or current iteration number must be provided, but not both at the same time"
        }

        val warmupIterations = if (config.nativeFork == NativeFork.PerIteration) 1 else config.warmups
        repeat(warmupIterations) { iteration ->
            val benchmarkNanos = config.iterationTime * config.iterationTimeUnit.toMultiplier()

            val metric = warmupSingleIteration(instance, benchmark, config.nativeGCAfterIteration,
                benchmarkNanos.nanoseconds, workers)

            val sample = metric.nanosToText(config.mode, config.outputTimeUnit)
            val iterationNumber = currentIteration ?: iteration
            reporter.output(name, benchmark.name, "Warm-up #$iterationNumber: $sample")
        }
    }

    private inline fun warmupSingleIterationLoop(
        synchronizer: MeasurementSynchronizer,
        nativeGCAfterIteration: Boolean,
        body: () -> Unit,
    ): Double {
        if (nativeGCAfterIteration)
            GC.collect()

        var iterations = 0L

        val duration = measureTime {
            while (!synchronizer.shouldStop) {
                body()
                iterations++
            }
            if (nativeGCAfterIteration)
                GC.collect()
        }

        return duration.toDouble(DurationUnit.NANOSECONDS) / iterations // TODO: metric
    }

    private fun <T> warmupSingleIteration(
        instance: T,
        benchmark: BenchmarkDescriptor<T>,
        nativeGCAfterIteration: Boolean,
        iterationDuration: Duration,
        workers: WorkersPool,
    ): Double {
        return singleIteration(instance, benchmark, workers, iterationDuration,{ results ->
            results.average()
        }) { sync, body ->
            warmupSingleIterationLoop(sync, nativeGCAfterIteration) {
                body()
            }
        }
    }

    private fun <T> measureSingleIteration(
        instance: T,
        benchmark: BenchmarkDescriptor<T>,
        iterationDuration: Duration,
        nativeGCAfterIteration: Boolean,
        workers: WorkersPool,
    ): Double {
        return singleIteration(instance, benchmark, workers, iterationDuration,{ it.average() }) { sync, body ->
            measureSingleIterationLoop(sync, nativeGCAfterIteration) {
                body()
            }
        }
    }

    @OptIn(ObsoleteWorkersApi::class)
    private inline fun <T, R> singleIteration(
        instance: T,
        benchmark: BenchmarkDescriptor<T>,
        workers: WorkersPool,
        iterationDuration: Duration,
        resultsAggregator: (List<R>) -> R,
        crossinline body: (MeasurementSynchronizer, () -> Unit) -> R
    ): R {
        val waiters = workers.numWorkers + 1 // + 1 is for the current thread
        val synchronizer = MeasurementSynchronizer()
        Barrier(waiters).use { barrier ->
            val runner = when (benchmark) {
                is BenchmarkDescriptorWithBlackholeParameter -> {
                    {
                        val blackhole = benchmark.blackhole
                        val delegate = benchmark.function
                        barrier.wait()
                        body(synchronizer) {
                            blackhole.consume(instance.delegate(blackhole))
                        }
                    }
                }

                is BenchmarkDescriptorWithNoBlackholeParameter -> {
                    {
                        val blackhole = benchmark.blackhole
                        val delegate = benchmark.function
                        barrier.wait()
                        body(synchronizer) {
                            blackhole.consume(instance.delegate())
                        }
                    }
                }

                else -> error("Unexpected ${benchmark::class.simpleName}")
            }

            // Submit single benchmark iteration to all workers
            val futures = workers.submit(runner)

            synchronizer.shouldStop = false
            // Synchronized workers
            barrier.wait()

            // TODO: is there a better way to sleep?
            usleep(iterationDuration.inWholeMicroseconds.convert())

            // We're done
            synchronizer.shouldStop = true

            // Await the end of iteration and aggregate results
            return resultsAggregator(futures.map { it.result })
        }
    }

    @OptIn(ObsoleteWorkersApi::class)
    private class WorkersPool(val numWorkers: Int): AutoCloseable {

        init {
            require(numWorkers > 0) {
                "At least one worker thread is required"
            }
        }

        private var closed = false
        private val workers = Array(numWorkers) { Worker.start(name = "Benchmark runner $it") }

        fun <R> submit(workload: () -> R): List<Future<R>> {
            check(!closed) { "Pools is closed!" }
            return workers.map { it.execute(TransferMode.UNSAFE, { workload }) { it() } }
        }

        override fun close() {
            if (closed) return
            closed = true
            workers.map { it.requestTermination(false) }.forEach {
                it.result
            }
        }
    }


    private fun resolveThreadsCount(threads: Int): Int {
        if (threads > 0) return threads
        require(threads == THREADS_CPU_COUNT) {
            "Illegal thread count value: $threads. It should be either positive, " +
                    "or equal to THREADS_CPU_COUNT ($THREADS_CPU_COUNT)"
        }
        return Platform.getAvailableProcessors()
    }

    private class MeasurementSynchronizer {
        @Volatile
        var shouldStop = false
    }
}

