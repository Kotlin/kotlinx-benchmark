package kotlinx.benchmark.wasm

import kotlinx.benchmark.*
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi


private external interface JsAny

@JsFun("(p) => p")
private external fun jsId(p: JsAny): JsAny

private fun id(p: Any): Any {
    // TODO: Use dedicated type for passing Kotlin references to JS when it is available
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return jsId(p as JsAny)
}

@KotlinxBenchmarkRuntimeInternalApi
class WasmBuiltInExecutor(
    name: String,
    @Suppress("UNUSED_PARAMETER") dummy_args: Array<out String>
) : CommonSuiteExecutor(name, jsEngineSupport.arguments()[0]) {

    private val BenchmarkConfiguration.notUseJsBridge: Boolean
        get() = "false".equals(advanced["jsUseBridge"], ignoreCase = true)

    @Suppress("UNCHECKED_CAST")
    private fun createJsMeasurerBridge(originalMeasurer: () -> Long): () -> Long =
        id(originalMeasurer) as (() -> Long)

    override fun <T> createIterationMeasurer(
        instance: T,
        benchmark: BenchmarkDescriptor<T>,
        configuration: BenchmarkConfiguration,
        cycles: Int
    ): () -> Long {
        val measurer = super.createIterationMeasurer(instance, benchmark, configuration, cycles)
        return if (configuration.notUseJsBridge) measurer else createJsMeasurerBridge(measurer)
    }
}