package kotlinx.benchmark.wasm

import kotlinx.benchmark.*
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@KotlinxBenchmarkRuntimeInternalApi
class SpawnBenchmarkExecutor(
    name: String,
    private val configPath: String,
    private val engineName: String,
    private val engineBinaryPath: String,
    private val engineArguments: String?,
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
        val scriptDirectory = modulePath.substringBeforeLast('/')

        val jsParameters =
            getJsParameters(engineArguments, modulePath, "$configPath $suiteIndex $benchmarkId")

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