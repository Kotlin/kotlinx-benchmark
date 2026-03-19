package kotlinx.benchmark.js

import kotlinx.benchmark.*
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@KotlinxBenchmarkRuntimeInternalApi
fun runBenchmarksBuiltIn(name: String, args: Array<out String>, declareAndExecuteSuites: (SuiteExecutorBase) -> Unit) {
    declareAndExecuteSuites(JsBuiltInExecutor(name, args))
}

private class JsBuiltInExecutor(
    name: String,
    @Suppress("UNUSED_PARAMETER") dummy_args: Array<out String>
) : SuiteExecutor(
    executionName = name,
    configPath = engineSupport.arguments()[0],
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