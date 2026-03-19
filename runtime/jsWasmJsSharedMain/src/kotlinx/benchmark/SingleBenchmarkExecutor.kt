package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@KotlinxBenchmarkRuntimeInternalApi
class SingleBenchmarkExecutor(
    override val executionName: String,
    private val runnerConfiguration: RunnerConfiguration,
    private val suiteIndex: Int,
    private val benchmarkId: String,
) : SuiteExecutorBase(), CommonBenchmarkExtension {
    override fun run() {
        val suiteToRun = suites[suiteIndex]
        val benchmarkConfiguration = BenchmarkConfiguration(runnerConfiguration, suiteToRun)

        runWithParameters(suiteToRun.parameters, runnerConfiguration.params, suiteToRun.defaultParameters) { parameters ->
            val benchmarkToRun = suiteToRun.benchmarks.firstOrNull { benchmark ->
                id(benchmark.name, parameters).replaceSpaceWithPercent() == benchmarkId
            } ?: return@runWithParameters

            val progress = BenchmarkProgress.create(runnerConfiguration.traceFormat, xml = null)

            @Suppress("UNCHECKED_CAST")
            val result = runBenchmark(benchmarkToRun as BenchmarkDescriptor<Any?>, benchmarkConfiguration, parameters, benchmarkId, progress)
            val stringifiedResult = result?.joinToString(separator = ",") { it.toRawBits().toString() } ?: ""
            println("<RESULT>$stringifiedResult</RESULT>")
        }
    }
}