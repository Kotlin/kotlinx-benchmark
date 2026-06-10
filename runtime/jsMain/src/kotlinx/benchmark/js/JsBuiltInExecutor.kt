package kotlinx.benchmark.js

import kotlinx.benchmark.*
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@KotlinxBenchmarkRuntimeInternalApi
fun runBenchmarksBuiltIn(name: String, args: Array<out String>, declareAndExecuteSuites: (SuiteExecutorBase) -> Unit) {
    val arguments = engineSupport.arguments()

    val executor = when {
        arguments.any { it == "startAll" } -> {
            val newArguments = arguments.filterNot { it == "startAll" }.toTypedArray()
            val configPath = newArguments[0]
            JsBuiltInExecutor(name, configPath)
        }
        arguments.any { it == "runSingle" } -> {
            val newArguments = arguments.filterNot { it == "runSingle" }.toTypedArray()
            val configPath = newArguments[0]
            val config = RunnerConfiguration(configPath.readFile())
            SingleBenchmarkExecutor(
                executionName = name,
                runnerConfiguration = config,
                suiteId = arguments[1],
                benchmarkId = arguments[2],
            )
        }
        else -> {
            val configPath = arguments[0]
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