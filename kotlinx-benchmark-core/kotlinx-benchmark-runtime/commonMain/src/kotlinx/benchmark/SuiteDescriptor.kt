package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@KotlinxBenchmarkRuntimeInternalApi
object DefaultDescriptorParameters {
    val iterations = 5
    val warmups = 5
    val iterationTime = IterationTime(10, BenchmarkTimeUnit.SECONDS)
    val outputTimeUnit = BenchmarkTimeUnit.SECONDS
    val mode = Mode.Throughput
}

@KotlinxBenchmarkRuntimeInternalApi
open class SuiteDescriptor<T>(
    val name: String,
    val factory: () -> T,
    val parametrize: (T, Map<String, String>) -> Unit,
    val setup: (T) -> Unit,
    val teardown: (T) -> Unit,

    val parameters: List<String>,
    val defaultParameters: Map<String, List<String>>,

    val iterations: Int = DefaultDescriptorParameters.iterations,
    val warmups: Int = DefaultDescriptorParameters.warmups,

    val iterationTime: IterationTime = DefaultDescriptorParameters.iterationTime,
    val outputTimeUnit: BenchmarkTimeUnit = DefaultDescriptorParameters.outputTimeUnit,
    val mode: Mode = DefaultDescriptorParameters.mode
) {
    private val _benchmarks = mutableListOf<BenchmarkDescriptor<T>>()

    val benchmarks: List<BenchmarkDescriptor<T>> get() = _benchmarks

    fun add(benchmark: BenchmarkDescriptor<T>) {
        _benchmarks.add(benchmark)
    }
}

@KotlinxBenchmarkRuntimeInternalApi
data class IterationTime(val value: Long, val timeUnit: BenchmarkTimeUnit)