package kotlinx.benchmark.wasm

import kotlinx.benchmark.*
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@KotlinxBenchmarkRuntimeInternalApi
fun runBenchmarks(name: String, @Suppress("unused") args: Array<out String>, declareAndExecuteSuites: (SuiteExecutorBase) -> Unit) {
    val arguments = StartArguments.parse(engineSupport.arguments())

    val executor = when(arguments.mode) {
        StartMode.Default, StartMode.StartAll -> {
            WasmWasiBuiltInExecutor(name, arguments.config)
        }
        StartMode.RunSingle -> {
            SingleBenchmarkExecutor(
                executionName = name,
                runnerConfiguration = RunnerConfiguration(arguments.config.readFile()),
                suiteId = arguments.suiteId ?: error("suiteId must be specified"),
                benchmarkId = arguments.benchmarkId ?: error("benchmarkId must be specified"),
            )
        }
        else -> {
            error("Unexpected arguments ${arguments.toArray().joinToString(" ")}")
        }
    }

    declareAndExecuteSuites(executor)
}