package test

import kotlinx.benchmark.*
import kotlin.math.*

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class AndroidTestBenchmark {
    private var data = 0.0

    @Setup
    fun setUp() {
        data = 3.0
    }

    @TearDown
    fun teardown() {
        // println("Teardown!")
    }

    @Benchmark
    fun sqrtBenchmark(): Double {
        return sqrt(data)
    }

    @Benchmark
    fun cosBenchmark(): Double {
        return cos(data)
    }
}