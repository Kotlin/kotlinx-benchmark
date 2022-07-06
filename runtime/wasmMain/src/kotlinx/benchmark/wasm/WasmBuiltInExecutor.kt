package kotlinx.benchmark.wasm

import kotlinx.benchmark.*

@JsFun("(p) => p")
private external fun id(p: Any): Any

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