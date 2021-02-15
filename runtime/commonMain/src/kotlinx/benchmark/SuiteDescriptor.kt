package kotlinx.benchmark

object DefaultDescriptorParameters {
    val iterations = 3
    val warmups = 3
    val iterationTime = IterationTime(1, BenchmarkTimeUnit.SECONDS)
}

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
    val outputTimeUnit: BenchmarkTimeUnit = BenchmarkTimeUnit.MILLISECONDS,
    val mode: Mode = Mode.Throughput
) {
    private val _benchmarks = mutableListOf<BenchmarkDescriptor<T>>()

    val benchmarks: List<BenchmarkDescriptor<T>> get() = _benchmarks

    fun add(benchmark: BenchmarkDescriptor<T>) {
        _benchmarks.add(benchmark)
    }
}

data class IterationTime(val value: Long, val timeUnit: BenchmarkTimeUnit)