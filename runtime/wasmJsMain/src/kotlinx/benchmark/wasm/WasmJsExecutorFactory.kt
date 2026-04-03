package kotlinx.benchmark.wasm

import kotlinx.benchmark.*
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@KotlinxBenchmarkRuntimeInternalApi
fun runBenchmarks(name: String, args: Array<out String>, declareAndExecuteSuites: (SuiteExecutorBase) -> Unit) {
    jsPromiseIntegration {
        runBenchmarksImpl(name, args, declareAndExecuteSuites)
    }
}

/**
 * Creates and runs a benchmark executor for the current Wasm environment.
 *
 * Expected [args] shape:
 * - `args` itself is not used directly; runtime arguments are taken from [engineSupport.arguments].
 * - first argument is the path to the benchmark runner configuration file
 * - optional second and third arguments identify a single benchmark run
 *
 * Execution modes:
 * - `arguments.size == 2`: run the full suite in the built-in Wasm engine; the second argument must be `"startAll"`
 * - `arguments.size == 3`: run one benchmark only, using the provided suite index and benchmark id
 * - `config.advanced["wasmFork"] == "perBenchmark"`: execute each benchmark in a separate spawned process
 * - otherwise: use the built-in engine unless custom engine binary / arguments are configured, in which case the custom engine is spawned for the full suite
 *
 * @param name the execution name reported to benchmark progress
 * @param args unused JVM-style entry arguments; runtime arguments are read from the Wasm host
 * @param declareAndExecuteSuites callback that declares benchmark suites and executes them with the selected executor
 */
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