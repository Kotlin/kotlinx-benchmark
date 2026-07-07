package kotlinx.benchmark.wasm

import kotlinx.benchmark.*
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@KotlinxBenchmarkRuntimeInternalApi
fun runBenchmarks(name: String, args: Array<out String>, declareAndExecuteSuites: (SuiteExecutorBase) -> Unit) {
    runBenchmarksImpl(name, args, declareAndExecuteSuites)
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
    val arguments = StartArguments.parse(engineSupport.arguments())
    val config = RunnerConfiguration(arguments.config.readFile())

    val engineName= config.advanced["customEngineName"] ?: "Custom Engine"
    val engineBinaryPath = config.advanced["customEngineBinaryPath"]
    val engineWorkingPath = config.advanced["customEngineWorkingDir"]

    var engineArgumentIndex = 0
    var engineArguments: MutableList<String>? = null
    while (true) {
        val argument = config.advanced["customEngineArgument_${engineArgumentIndex}"] ?: break
        engineArguments = engineArguments ?: mutableListOf()
        engineArguments.add(argument)
        engineArgumentIndex++
    }

    val executor = when (arguments.mode) {
        StartMode.StartAll -> {
            WasmBuiltInExecutor(name, arguments.config)
        }

        StartMode.RunSingle -> {
            SingleBenchmarkExecutor(
                executionName = name,
                runnerConfiguration = config,
                suiteId = arguments.suiteId ?: error("suiteId must be specified"),
                benchmarkId = arguments.benchmarkId ?: error("benchmarkId must be specified"),
            )
        }

        StartMode.RunSingleWithOutputSplitter -> {
            val modulePath = nodeJsEngineModulePath()
            val engineBinaryPath = engineBinaryPath ?: nodeJsEngineBinaryPath()
            val engineWorkingPath = engineWorkingPath ?: nodeJsGetDirName(modulePath)
            val jsArguments = getJsParameters(engineArguments, modulePath, arguments.copy(mode = StartMode.RunSingle))

            println("Spawning $engineName...")
            spawnProcessAsyncAndProcessTags(
                binaryPath = engineBinaryPath,
                workingDir = engineWorkingPath,
                engineArguments = jsArguments,
                processResultTags = true
            )
            return
        }

        StartMode.Default -> {
            when {
                config.advanced["wasmFork"] == "perBenchmark" -> {
                    SpawnBenchmarkExecutor(name = name, configPath = arguments.config)
                }

                engineBinaryPath == null && engineArguments == null -> {
                    WasmBuiltInExecutor(name, arguments.config)
                }

                else  -> {
                    val modulePath = nodeJsEngineModulePath()
                    val scriptDirectory = engineWorkingPath ?: nodeJsGetDirName(modulePath)
                    val jsParameters = getJsParameters(
                        engineArguments,
                        modulePath,
                        StartArguments(arguments.config, StartMode.StartAll)
                    )
                    println("Spawning $engineName...")
                    spawnProcessAsyncAndProcessTags(
                        binaryPath = engineBinaryPath ?: nodeJsEngineBinaryPath(),
                        workingDir = scriptDirectory,
                        engineArguments = jsParameters,
                        processResultTags = false,
                    )
                    return
                }
            }
        }
    }

    declareAndExecuteSuites(executor)
}