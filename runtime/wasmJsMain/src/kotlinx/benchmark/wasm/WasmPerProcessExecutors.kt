package kotlinx.benchmark.wasm

import kotlinx.benchmark.*

private fun execArgv(): String =
    js("process.execArgv.join(' ')")

internal fun nodeJsExecArgv(): List<String> {
    val joined: String = execArgv()
    return joined.split(" ")
}

/**
 * Executes benchmarks by spawning a separate engine process for each run.
 */
internal class SpawnBenchmarkExecutor(
    name: String,
    private val configPath: String,
    xmlReporter: (() -> BenchmarkProgress)? = null,
) : RunAllBenchmarksExtension, SuiteExecutor(name, configPath, xmlReporter) {
    override fun runBenchmark(
        benchmark: BenchmarkDescriptor<Any?>,
        configuration: BenchmarkConfiguration,
        parameters: Map<String, String>,
        id: String,
        progress: BenchmarkProgress,
    ): DoubleArray? {
        val benchmarkId = id.replaceSpaceWithPercent()
        val suiteId = benchmark.suite.name.replaceSpaceWithPercent()

        val nodeJsPath = nodeJsEngineBinaryPath()
        val modulePath = nodeJsEngineModulePath()
        val workingDir = nodeJsGetDirName(modulePath)

        val engineArguments = nodeJsExecArgv() + listOf("<MODULE>", "<ARGUMENTS>")
        val arguments = StartArguments(configPath, StartMode.RunSingleWithOutputSplitter, suiteId, benchmarkId)
        val jsParameters = getJsParameters(
            engineArguments = engineArguments,
            modulePath = modulePath,
            startArguments = arguments
        )

        val result = jsSpawnProcessWithExtraPipeSyncAndGetResult(
            childProcess = childProcess,
            binaryPath = nodeJsPath,
            workingDir = workingDir,
            engineArguments = jsParameters,
        )

        if (result.isNullOrBlank()) return null

        return result
            .split(',')
            .map { Double.fromBits(it.toLong()) }
            .toDoubleArray()
    }

    override fun run(
        runnerConfiguration: RunnerConfiguration,
        benchmarks: List<BenchmarkDescriptor<Any?>>,
        start: () -> Unit,
        complete: () -> Unit
    ) = super<RunAllBenchmarksExtension>.run(runnerConfiguration, benchmarks, start, complete)
}