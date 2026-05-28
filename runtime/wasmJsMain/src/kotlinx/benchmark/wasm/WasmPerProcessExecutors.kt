package kotlinx.benchmark.wasm

import kotlinx.benchmark.*

/**
 * Executes benchmarks by spawning a separate engine process for each run.
 */
internal class SpawnBenchmarkExecutor(
    name: String,
    private val configPath: String,
    private val engineName: String,
    private val engineBinaryPath: String,
    private val engineWorkingDir: String?,
    private val engineArguments: List<String>?,
    xmlReporter: (() -> BenchmarkProgress)? = null,
) : RunAllBenchmarksExtension, SuiteExecutor(name, configPath, xmlReporter) {
    override fun runBenchmark(
        benchmark: BenchmarkDescriptor<Any?>,
        configuration: BenchmarkConfiguration,
        parameters: Map<String, String>,
        id: String,
        progress: BenchmarkProgress,
    ): DoubleArray? {
        val suiteIndex = suites.indexOf(benchmark.suite)
        check(suiteIndex >= 0)
        val benchmarkId = id.replaceSpaceWithPercent()
        val modulePath = nodeJsEngineModulePath()
        val scriptDirectory = engineWorkingDir ?: nodeJsGetDirName(modulePath)

        val jsParameters =
            getJsParameters(engineArguments, modulePath, listOf(configPath, suiteIndex.toString(), benchmarkId))

        val result = spawnProcessAndGetResult(engineBinaryPath, scriptDirectory, jsParameters)

        return result
            ?.split(',')
            ?.map { Double.fromBits(it.toLong()) }
            ?.toDoubleArray()
    }

    override fun run(
        runnerConfiguration: RunnerConfiguration,
        benchmarks: List<BenchmarkDescriptor<Any?>>,
        start: () -> Unit,
        complete: () -> Unit) {
            println("Spawning $engineName...")
            super<RunAllBenchmarksExtension>.run(runnerConfiguration, benchmarks, start, complete)
        }
}