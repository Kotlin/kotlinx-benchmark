package kotlinx.benchmark.wasm

import kotlinx.benchmark.*
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@KotlinxBenchmarkRuntimeInternalApi
fun runBenchmarks(name: String, args: Array<out String>, declareAndExecuteSuites: (SuiteExecutorBase) -> Unit) {
    jsPromiseIntegration {
        runBenchmarksImpl(name, args, declareAndExecuteSuites)
    }
}

internal fun runBenchmarksImpl(name: String, @Suppress("unused") args: Array<out String>, declareAndExecuteSuites: (SuiteExecutorBase) -> Unit) {
    val arguments = engineSupport.arguments()
    val configPath = arguments[0]
    val config = RunnerConfiguration(configPath.readFile())

    val engineName= config.advanced["customEngineName"] ?: "Custom Engine"
    val engineBinaryPath = config.advanced["customEngineBinaryPath"]
    val engineArguments = config.advanced["customEngineArguments"]

    val executor = when {
        arguments.size == 2 -> {
            check (arguments[1] == "startAll")
            WasmBuiltInExecutor(name, configPath)
        }

        arguments.size == 3 -> {
            SingleBenchmarkExecutor(
                executionName = name,
                runnerConfiguration = config,
                suiteIndex = arguments[1].toInt(),
                benchmarkId = arguments[2],
            )
        }

        arguments.size > 3 -> {
            error("Unexpected ${arguments.size} argument(s)")
        }

        config.advanced["wasmFork"] != "perBenchmark" -> {
            if (engineBinaryPath == null && engineArguments == null) {
                WasmBuiltInExecutor(name, configPath)
            } else {
                val modulePath = nodeJsEngineModulePath()
                val scriptDirectory = modulePath.substringBeforeLast('/')
                val jsParameters = getJsParameters(engineArguments, modulePath, "$configPath startAll")
                println("Spawning $engineName...")
                spawnProcess(
                    binaryPath = engineBinaryPath ?: nodeJsEngineBinaryPath(),
                    workingDir = scriptDirectory,
                    engineArguments = jsParameters,
                )
                return
            }
        }

        else -> {
            SpawnBenchmarkExecutor(
                name = name,
                configPath = configPath,
                engineName = engineName,
                engineBinaryPath = engineBinaryPath ?: nodeJsEngineBinaryPath(),
                engineArguments = engineArguments,
            )
        }
    }

    declareAndExecuteSuites(executor)
}