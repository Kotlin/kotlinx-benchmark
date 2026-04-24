package kotlinx.benchmark.wasm

import kotlinx.benchmark.*

/**
 * Executes benchmarks in the built-in Wasm/WASI engine.
 */
internal class WasmWasiBuiltInExecutor(name: String, configPath: String, xmlReporter: (() -> BenchmarkProgress)? = null) :
    SuiteExecutor(name, configPath, xmlReporter),
    RunAllBenchmarksExtension, CommonBenchmarkExtension {

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
    ) = super<RunAllBenchmarksExtension>.run(runnerConfiguration, benchmarks, start, complete)
}