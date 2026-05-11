package kotlinx.benchmark.js

import kotlinx.benchmark.*
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@KotlinxBenchmarkRuntimeInternalApi
fun runBenchmarksBuiltIn(name: String, args: Array<out String>, declareAndExecuteSuites: (SuiteExecutorBase) -> Unit) {
    val arguments = engineSupport.arguments()
    val configPath = arguments[0]

    val executor = when (arguments.size) {
        2 -> {
            check(arguments[1] == "startAll")
            JsBuiltInExecutor(name, configPath)
        }
        3 -> {
            SingleBenchmarkExecutor(
                executionName = name,
                runnerConfiguration = RunnerConfiguration(configPath.readFile()),
                suiteIndex = arguments[1].toInt(),
                benchmarkId = arguments[2],
            )
        }
        else -> {
            JsBuiltInExecutor(name, configPath)
        }
    }

    declareAndExecuteSuites(executor)
}

private class JsBuiltInExecutor(
    name: String,
    configPath: String,
) : SuiteExecutor(
    executionName = name,
    configPath = configPath,
), RunAllBenchmarksExtension, CommonBenchmarkExtension  {

    private val BenchmarkConfiguration.notUseJsBridge: Boolean
        get() = "false".equals(advanced["jsUseBridge"], ignoreCase = true)

    private fun createJsMeasurerBridge(originalMeasurer: () -> Long): () -> Long =
        { originalMeasurer() }

    override fun <T> createIterationMeasurer(
        instance: T,
        benchmark: BenchmarkDescriptor<T>,
        configuration: BenchmarkConfiguration,
        cycles: Int
    ): () -> Long {
        val measurer = super.createIterationMeasurer(instance, benchmark, configuration, cycles)
        return if (configuration.notUseJsBridge) measurer else createJsMeasurerBridge(measurer)
    }

    override fun runBenchmark(
        benchmark: BenchmarkDescriptor<Any?>,
        configuration: BenchmarkConfiguration,
        parameters: Map<String, String>,
        id: String,
        progress: BenchmarkProgress
    ): DoubleArray? = super.runBenchmark(benchmark, configuration, parameters, id, progress)

    override fun run(
        runnerConfiguration: RunnerConfiguration,
        benchmarks: List<BenchmarkDescriptor<Any?>>,
        start: () -> Unit,
        complete: () -> Unit
    ) {
        if (benchmarks.any { it.isAsync }) {
            error("${JsBuiltInExecutor::class.simpleName} does not support async functions")
        }
        super<RunAllBenchmarksExtension>.run(runnerConfiguration, benchmarks, start, complete)
    }
}