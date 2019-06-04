package org.jetbrains.gradle.benchmarks

open class SuiteDescriptor<T>(
    val name: String,
    val factory: () -> T,
    val setup: (T) -> Unit,
    val teardown: (T) -> Unit,

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