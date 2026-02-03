package test

import kotlinx.benchmark.*
import kotlin.math.*

@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@Measurement(iterations = 1, time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
open class SimpleBenchmark {
    @Benchmark
    fun bm() = 42
}
