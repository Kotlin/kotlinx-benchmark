package test

import kotlinx.benchmark.*
import kotlin.math.*

@State(Scope.Benchmark)
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