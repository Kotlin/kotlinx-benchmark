package kotlinx.benchmark.wasm

import kotlinx.benchmark.*
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import kotlinx.benchmark.wasm.wasi.wasiGetArguments

@KotlinxBenchmarkRuntimeInternalApi
fun runBenchmarks(name: String, @Suppress("unused") args: Array<out String>, declareAndExecuteSuites: (SuiteExecutorBase) -> Unit) {
    val arguments = wasiGetArguments()
    check (arguments.size >= 3) { "Unexpected number of arguments: ${arguments.size}"}
    val configPath = arguments[2]

    val executor = when (arguments.size) {
        3 -> {
            WasmWasiBuiltInExecutor(name, configPath)
        }
        4 -> {
            check (arguments[3] == "startAll")
            WasmWasiBuiltInExecutor(name, configPath)
        }
        5 -> {
            val config = RunnerConfiguration(configPath.readFile())
            SingleBenchmarkExecutor(
                executionName = name,
                runnerConfiguration = config,
                suiteIndex = arguments[3].toInt(),
                benchmarkId = arguments[4],
            )
        }
        else -> error("Unexpected number of arguments: ${arguments.size}")
    }

    declareAndExecuteSuites(executor)
}