package test.nested

import kotlinx.benchmark.*
import kotlin.math.*

/**
 * This benchmark is to test that benchmarks with the same name but located in different packages
 * are handled correctly by the benchmarking plugin.
 */
@State(Scope.Benchmark)
@Measurement(iterations = 3, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
class CommonBenchmark {
    private var data = 0.0

    @Setup
    fun setUp() {
        data = 3.0
    }

    @Benchmark
    fun mathBenchmark(): Double {
        return log(sqrt(data) * cos(data), 2.0)
    }
}