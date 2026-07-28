package kotlinx.benchmark.wasm

import kotlinx.benchmark.*
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import kotlin.collections.plus

private external interface JsAny

// Parse native syntax lazily so the executor can report a missing V8 flag instead of failing module loading.
private fun isNativeSyntaxEnabled(): Boolean = js(
        // language="JavaScript"
        """
        () => {
            try {
                new Function("f", "return %IsLiftoffFunction(f)");
                new Function("f", "return %IsTurboFanFunction(f)");
                new Function("f", "return %IsUncompiledWasmFunction(f)");
                return true;
            } catch (_) {
                return false;
            }
        }
        """
)

private fun wasmCompilationTier(@Suppress("unused") f: JsAny): String = js(
        // language="JavaScript"
        """
        (f) => new Function("f", `
            if (%IsLiftoffFunction(f)) return "Liftoff";
            if (%IsTurboFanFunction(f)) return "TurboFan";
            if (%IsUncompiledWasmFunction(f)) return "Uncompiled";
            return "Unknown";
        `)(f)
    """
)

private fun wasmBenchmarkInvocation(): JsAny = js(
    // language="JavaScript"
    "() => wasmExports.wasmBenchmarkTierProbeInvocation"
)

private fun invokeJsFunction(@Suppress("unused") f: JsAny) {
    js(
        // language="JavaScript"
        "(f) => f()"
    )
}

// V8's tier intrinsics only accept an actual Wasm export, not Kotlin's JavaScript function wrapper.
private var activeBenchmarkInvocation: (() -> Long)? = null
private var activeBenchmarkInvocationResult = 0L

@OptIn(ExperimentalJsExport::class)
@JsExport
@KotlinxBenchmarkRuntimeInternalApi
fun wasmBenchmarkTierProbeInvocation() {
    activeBenchmarkInvocationResult = checkNotNull(activeBenchmarkInvocation)()
}

/**
 * Executes benchmarks in the built-in Wasm engine.
 */
internal class WasmBuiltInExecutor(name: String, configPath: String, xmlReporter: (() -> BenchmarkProgress)? = null) :
    SuiteExecutor(name, configPath, xmlReporter),
    RunAllBenchmarksExtension, CommonBenchmarkExtension {

    private companion object {
        const val COMPILATION_TIER = "wasmCompilationTier"
        const val WARMUP_COMPILATION_TIER = "wasmWarmupCompilationTier"
    }

    private var warmupCompilationTier: String? = null
    private val measurementCompilationTiers = mutableListOf<String>()

    init {
        check(isNativeSyntaxEnabled()) {
            "V8 native syntax is required to report Wasm compilation tiers. " +
                    "Run the engine with --allow-natives-syntax."
        }
    }

    private val BenchmarkConfiguration.notUseJsBridge: Boolean
    get() = "false".equals(advanced["jsUseBridge"], ignoreCase = true)

    private fun createJsMeasurerBridge(originalMeasurer: () -> Long): () -> Long {
        activeBenchmarkInvocation = originalMeasurer
        val invocation = wasmBenchmarkInvocation()
        return {
            invokeJsFunction(invocation)
            activeBenchmarkInvocationResult
        }
    }

    override fun <T> createIterationMeasurer(
        instance: T,
        benchmark: BenchmarkDescriptor<T>,
        configuration: BenchmarkConfiguration,
        cycles: Int
    ): () -> Long {
        val measurer = super.createIterationMeasurer(instance, benchmark, configuration, cycles)
        return if (configuration.notUseJsBridge) measurer else createJsMeasurerBridge(measurer)
    }

    override fun runBenchmark(
        benchmark: BenchmarkDescriptor<Any?>,
        configuration: BenchmarkConfiguration,
        parameters: Map<String, String>,
        id: String,
        progress: BenchmarkProgress
    ): DoubleArray? {
        check(!configuration.notUseJsBridge) {
            "Wasm compilation tier reporting requires the JavaScript measurer bridge"
        }
        warmupCompilationTier = null
        measurementCompilationTiers.clear()
        return super.runBenchmark(benchmark, configuration, parameters, id, progress)
    }

    override fun warmup(
        id: String,
        configuration: BenchmarkConfiguration,
        cycles: Int,
        measurer: () -> Long,
        progress: BenchmarkProgress,
    ) {
        super.warmup(id, configuration, cycles, measurer, progress)
        warmupCompilationTier = currentCompilationTier()
        progress.output(executionName, id, "Compilation tier after warm-up: $warmupCompilationTier")
    }

    override fun measure(
        id: String,
        configuration: BenchmarkConfiguration,
        cycles: Int,
        measurer: () -> Long,
        progress: BenchmarkProgress,
    ): DoubleArray = DoubleArray(configuration.iterations) { iteration ->
        val nanosecondsPerOperation = measurer().toDouble() / cycles
        val tier = currentCompilationTier()
        measurementCompilationTiers.add(tier)
        val text = nanosecondsPerOperation.nanosToText(configuration.mode, configuration.outputTimeUnit)
        progress.output(executionName, id, "Iteration #$iteration [$tier]: $text")
        nanosecondsPerOperation
    }

    override fun saveBenchmarkResults(
        benchmark: BenchmarkDescriptor<Any?>,
        configuration: BenchmarkConfiguration,
        parameters: Map<String, String>,
        id: String,
        samples: DoubleArray,
    ) {
        check(samples.size == measurementCompilationTiers.size) {
            "Expected one Wasm compilation tier per measurement sample"
        }

        val warmupTier = checkNotNull(warmupCompilationTier)
        val resultsByTier = measurementCompilationTiers.distinct().map { tier ->
            val tierSamples = samples.filterIndexed { index, _ -> measurementCompilationTiers[index] == tier }
                .map { it.nanosToSample(configuration.mode, configuration.outputTimeUnit) }
                .toDoubleArray()
            val tierConfiguration = configuration
                .withAdvanced(WARMUP_COMPILATION_TIER, warmupTier)
                .withAdvanced(COMPILATION_TIER, tier)
            val tierParameters = parameters + (COMPILATION_TIER to tier)
            tier to ReportBenchmarksStatistics.createResult(benchmark, tierParameters, tierConfiguration, tierSamples)
        }

        val message = resultsByTier.joinToString(separator = "\n") { (tier, result) ->
            with(result) {
                "$tier: ~ ${score.sampleToText(config.mode, config.outputTimeUnit)} " +
                        "±${(error / score * 100).formatSignificant(2)}%"
            }
        }
        reporter.endBenchmark(executionName, id, BenchmarkProgress.FinishStatus.Success, message)
        resultsByTier.forEach { (_, result) -> result(result) }
    }

    private fun currentCompilationTier(): String = wasmCompilationTier(wasmBenchmarkInvocation())

    override fun run(
        runnerConfiguration: RunnerConfiguration,
        benchmarks: List<BenchmarkDescriptor<Any?>>,
        start: () -> Unit,
        complete: () -> Unit
    ) = super<RunAllBenchmarksExtension>.run(runnerConfiguration, benchmarks, start, complete)
}