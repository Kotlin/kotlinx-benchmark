package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

internal object DefaultDescriptorParameters {
    val iterations = 5
    val warmups = 5
    val iterationTime = IterationTime(10, BenchmarkTimeUnit.SECONDS)
    val outputTimeUnit = BenchmarkTimeUnit.SECONDS
    val mode = Mode.Throughput
    val threads = 1
}

@KotlinxBenchmarkRuntimeInternalApi
public open class SuiteDescriptor<T>(
    public val name: String,
    public val factory: () -> T,
    public val parametrize: (T, Map<String, String>) -> Unit,
    public val setup: (T) -> Unit,
    public val teardown: (T) -> Unit,

    public val parameters: List<String>,
    public val defaultParameters: Map<String, List<String>>,

    public val iterations: Int = DefaultDescriptorParameters.iterations,
    public val warmups: Int = DefaultDescriptorParameters.warmups,

    public val iterationTime: IterationTime = DefaultDescriptorParameters.iterationTime,
    public val outputTimeUnit: BenchmarkTimeUnit = DefaultDescriptorParameters.outputTimeUnit,
    public val mode: Mode = DefaultDescriptorParameters.mode,
    public val threads: Int = DefaultDescriptorParameters.threads,
) {
    private val _benchmarks = mutableListOf<BenchmarkDescriptor<T>>()

    public val benchmarks: List<BenchmarkDescriptor<T>> get() = _benchmarks

    public fun add(benchmark: BenchmarkDescriptor<T>) {
        _benchmarks.add(benchmark)
    }
}

@KotlinxBenchmarkRuntimeInternalApi
public data class IterationTime(val value: Long, val timeUnit: BenchmarkTimeUnit)
