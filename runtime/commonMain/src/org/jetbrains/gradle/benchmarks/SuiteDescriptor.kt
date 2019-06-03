package org.jetbrains.gradle.benchmarks

open class SuiteDescriptor<T>(
    val name: String,
    val factory: () -> T,
    val setup: (T) -> Unit,
    val teardown: (T) -> Unit,

    val iterations: Int,
    val warmups: Int,

    val iterationTime: Pair<Int, BenchmarkTimeUnit>,
    val outputUnit: BenchmarkTimeUnit,
    val mode: Mode
) {
    private val _benchmarks = mutableListOf<BenchmarkDescriptor<T>>()

    val benchmarks: List<BenchmarkDescriptor<T>> get() = _benchmarks

    fun add(benchmark: BenchmarkDescriptor<T>) {
        _benchmarks.add(benchmark)
    }
}