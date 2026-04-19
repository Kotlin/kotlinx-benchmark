package test

import kotlinx.benchmark.*
import kotlinx.datetime.*
import kotlin.math.*
import kotlin.time.*

@State(Scope.Benchmark)
// Warmup and Measurement annotations are ignored on Android
// @Warmup(iterations = 3, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
// @Measurement(iterations = 3, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
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

    @Benchmark
    fun localDate(): LocalDate {
        return LocalDate.fromEpochDays(100)
    }
}