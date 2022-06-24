package kotlinx.benchmark.js

import kotlinx.benchmark.*
import kotlinx.benchmark.jsEngineSupport

@JsName("Function")
private external fun functionCtor(params: String, code: String): (dynamic) -> Long

class JsBuiltInExecutor(
    name: String,
    @Suppress("UNUSED_PARAMETER") dummy_args: Array<out String>
) : CommonSuiteExecutor(name, jsEngineSupport.arguments()[0]) {

    private val BenchmarkConfiguration.jsUseBridge: Boolean
        get() = "true".equals(advanced["jsUseBridge"], ignoreCase = true)

    override fun run(
        runnerConfiguration: RunnerConfiguration,
        benchmarks: List<BenchmarkDescriptor<Any?>>,
        start: () -> Unit,
        complete: () -> Unit
    ) {
        if (benchmarks.any { it.isAsync }) {
            error("${JsBuiltInExecutor::class.simpleName} does not supports async functions")
        }
        super.run(runnerConfiguration, benchmarks, start, complete)
    }

    private fun createJsMeasurerBridge(originalMeasurer: () -> Long): () -> Long {
        val bridgeObject = object {
            fun invoke(): Long = originalMeasurer.invoke()
        }
        val measurerString = bridgeObject::invoke.toString()
        val measurerBody = measurerString.substringAfter("{").substringBeforeLast("}")
        return {
            functionCtor("\$boundThis", measurerBody)(bridgeObject)
        }
    }

    override fun <T> createIterationMeasurer(
        instance: T,
        benchmark: BenchmarkDescriptor<T>,
        configuration: BenchmarkConfiguration,
        cycles: Int
    ): () -> Long {
        val measurer = super.createIterationMeasurer(instance, benchmark, configuration, cycles)
        return if (configuration.jsUseBridge) createJsMeasurerBridge(measurer) else measurer
    }
}