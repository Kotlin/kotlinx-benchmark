package kotlinx.benchmark.native

import kotlinx.benchmark.*
import kotlin.native.internal.GC
import kotlin.system.*

class NativeExecutor(name: String, args: Array<out String>) : SuiteExecutor(name, args) {

    data class BenchmarkRun(val benchmarkName: String, val config: BenchmarkConfiguration,
                            val parameters: Map<String, String>)

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
                val fileName = "${additionalArguments[1]}/${suite.name}_${it.name}_$parametersId.txt"
                writeFile(fileName, benchmarkRunConfig)
                parametersId++
            }
        }
    }

    private fun List<BenchmarkDescriptor<Any?>>.getBenchmark(name: String) = find { it.name == name }
        ?: throw NoSuchElementException("Benchmark $name wasn't found.")

    private fun runBenchmarkIteration(benchmarks: List<BenchmarkDescriptor<Any?>>) {
        val (configFileName, iteration, resultsFile) = additionalArguments
        val benchmarkRun = configFileName.parseBenchmarkConfig()
        val benchmark = benchmarks.getBenchmark(benchmarkRun.benchmarkName)
        val samples = run(benchmark, benchmarkRun, iteration.toInt())
        writeFile(resultsFile, samples?.let{ it[0].toString() } ?: "null")
    }

    private fun runBenchmarkWarmup(benchmarks: List<BenchmarkDescriptor<Any?>>) {
        val (_, configFileName, iteration, resultsFile) = additionalArguments
        val benchmarkRun = configFileName.parseBenchmarkConfig()
        val benchmark = benchmarks.getBenchmark(benchmarkRun.benchmarkName)
        val id = id(benchmark.name, benchmarkRun.parameters)
        if (iteration.toInt() == 0) {
            reporter.startBenchmark(executionName, id)
        }
        val instance = benchmark.suite.factory() // TODO: should we create instance per bench or per suite?
        benchmark.suite.parametrize(instance, benchmarkRun.parameters)
        benchmark.suite.setup(instance)
        try {
            warmup(benchmark.suite.name, benchmarkRun.config, benchmark.suite.factory(), benchmark, iteration.toInt())
        } catch (e: Throwable) {
            val error = e.toString()
            val stacktrace = e.stacktrace()
            reporter.endBenchmarkException(executionName, id, error, stacktrace)
            writeFile(resultsFile, "null")
        } finally {
            benchmark.suite.teardown(instance)
        }
    }

    private fun runBenchmark(benchmarks: List<BenchmarkDescriptor<Any?>>) {
        val (_, configFileName, resultsFile) = additionalArguments
        val benchmarkRun = configFileName.parseBenchmarkConfig()
        val benchmark = benchmarks.getBenchmark(benchmarkRun.benchmarkName)
        val id = id(benchmark.name, benchmarkRun.parameters)
        reporter.startBenchmark(executionName, id)
        val samples = run(benchmark, benchmarkRun)
        if (samples != null) {
            saveBenchmarkResults(benchmark, benchmarkRun, samples, resultsFile)
        }
    }

    fun run(benchmark: BenchmarkDescriptor<Any?>, benchmarkRun: BenchmarkRun,
            externalIterationNumber: Int? = null): DoubleArray?  {
        val id = id(benchmark.name, benchmarkRun.parameters)
        val suite = benchmark.suite

        val instance = suite.factory() // TODO: should we create instance per bench or per suite?
        suite.parametrize(instance, benchmarkRun.parameters)
        benchmark.suite.setup(instance)

        var exception: Throwable? = null
        val iterations = if (benchmarkRun.config.nativeIterationMode == NativeIterationMode.External) 1 else benchmarkRun.config.iterations
        val samples = try {
            // Execute warmup
            val cycles = warmup(suite.name, benchmarkRun.config, instance, benchmark)
            DoubleArray(iterations) { iteration ->
                val nanosecondsPerOperation = measure(instance, benchmark, cycles)
                val text = nanosecondsPerOperation.nanosToText(benchmarkRun.config.mode, benchmarkRun.config.outputTimeUnit)
                val iterationNumber = externalIterationNumber ?: iteration
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
        }
        exception?.let {
            val error = exception.toString()
            val stacktrace = exception.stacktrace()
            reporter.endBenchmarkException(executionName, id, error, stacktrace)
            return null
        } ?: return samples
    }

    private fun saveBenchmarkResults(benchmark: BenchmarkDescriptor<Any?>, benchmarkRun: BenchmarkRun,
                              samples: DoubleArray, fileName: String? = null) {
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
        fileName?.let { writeFile(it, JsonBenchmarkReportFormatter.format(result)) }
    }

    fun storeResults(benchmarks: List<BenchmarkDescriptor<Any?>>) {
        val (configFileName, fileName) = additionalArguments
        val benchmarkRun = configFileName.parseBenchmarkConfig()
        val benchmark = benchmarks.getBenchmark(benchmarkRun.benchmarkName)
        val samples = fileName.readFile().split(", ").map { it.toDouble() }.toDoubleArray()
        saveBenchmarkResults(benchmark, benchmarkRun, samples, fileName)
    }

    override fun run(
        runnerConfiguration: RunnerConfiguration,
        reporter: BenchmarkProgress,
        benchmarks: List<BenchmarkDescriptor<Any?>>,
        start: () -> Unit,
        complete: () -> Unit
    ) {
        when(additionalArguments.size) {
            1 -> complete()
            2 -> when (additionalArguments.first()) {
                "--list" -> outputBenchmarks(runnerConfiguration, benchmarks, start)
                else -> storeResults(benchmarks)
            }
            3 -> when (additionalArguments.first()) {
                "--internal" -> runBenchmark(benchmarks)
                else -> runBenchmarkIteration(benchmarks)
            }
            4 -> runBenchmarkWarmup(benchmarks)

        }
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
        GC.collect()
        val startTime = getTimeNanos()
        while (counter-- > 0) {
            @Suppress("UNUSED_VARIABLE")
            val result = instance.executeFunction() // ignore result for now, but might need to consume it somehow
        }
        GC.collect()
        val endTime = getTimeNanos()
        val time = endTime - startTime
        return time.toDouble() / cycles
    }

    private fun <T> warmup(
        name: String,
        config: BenchmarkConfiguration,
        instance: T,
        benchmark: BenchmarkDescriptor<T>,
        currentIteration: Int? = null
    ): Int {
        var iterations = 0
        val warmupIterations = if (config.nativeIterationMode == NativeIterationMode.External) 1 else config.warmups
        repeat(warmupIterations) { iteration ->
            val benchmarkNanos = config.iterationTime * config.iterationTimeUnit.toMultiplier()
            val executeFunction = benchmark.function
            GC.collect()
            val startTime = getTimeNanos()
            var endTime = startTime
            iterations = 0
            while (endTime - startTime < benchmarkNanos) {
                instance.executeFunction()
                endTime = getTimeNanos()
                iterations++
            }
            GC.collect()
            val time = endTime - startTime
            val metric = time.toDouble() / iterations // TODO: metric
            val sample = metric.nanosToText(config.mode, config.outputTimeUnit)
            val iterationNumber = currentIteration ?: iteration
            if (config.nativeIterationMode == NativeIterationMode.Internal || currentIteration != null)
                reporter.output(name, benchmark.name, "Warm-up #$iterationNumber: $sample")
        }
        return iterations
    }
}

