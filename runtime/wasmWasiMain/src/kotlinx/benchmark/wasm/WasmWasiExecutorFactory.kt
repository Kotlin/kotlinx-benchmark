package kotlinx.benchmark.wasm

import kotlinx.benchmark.*
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import kotlinx.benchmark.wasm.wasi.wasiGetArguments

@KotlinxBenchmarkRuntimeInternalApi
fun runBenchmarks(name: String, @Suppress("unused") args: Array<out String>, declareAndExecuteSuites: (SuiteExecutorBase) -> Unit) {
    val arguments = wasiGetArguments()

    val executor = when {
        arguments.any { it == "startAll" } -> {
            val newArguments = arguments.filterNot { it == "startAll" }.toTypedArray()
            val configPath = newArguments[2]
            WasmWasiBuiltInExecutor(name, configPath)
        }
        arguments.any { it == "runSingle" } -> {
            val newArguments = arguments.filterNot { it == "runSingle" }.toTypedArray()
            val configPath = newArguments[2]
            val config = RunnerConfiguration(configPath.readFile())
            SingleBenchmarkExecutor(
                executionName = name,
                runnerConfiguration = config,
                suiteId = arguments[3],
                benchmarkId = arguments[4],
            )
        }
        else -> {
            val configPath = arguments[2]
            WasmWasiBuiltInExecutor(name, configPath)
        }
    }

    declareAndExecuteSuites(executor)
}