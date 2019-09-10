package kotlinx.benchmark

open class SuiteDescriptor<T>(
    val name: String,
    val factory: () -> T,
    val parametrize: (T, Map<String, String>) -> Unit,

    val hasInvocationFixture: Boolean,

    val invocationSetup: (T) -> Unit,
    val iterationSetup: (T) -> Unit,
    val trialSetup: (T) -> Unit,

    val invocationTearDown: (T) -> Unit,
    val iterationTearDown: (T) -> Unit,
    val trialTearDown: (T) -> Unit,

    val parameters: List<String>,
    val defaultParameters: Map<String, List<String>>,

    val iterations: Int = 3,
    val warmups: Int = 3,

    val iterationTime: IterationTime = IterationTime(1, BenchmarkTimeUnit.SECONDS),
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