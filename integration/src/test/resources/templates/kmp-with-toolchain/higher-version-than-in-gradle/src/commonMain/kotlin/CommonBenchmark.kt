package test

import kotlinx.benchmark.*
import kotlin.math.*

// Don't really need to measure anything here, just check that the benchmark works
@State(Scope.Benchmark)
@Warmup(iterations = 0)
@Measurement(iterations = 1, time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
open class CommonBenchmark {
    @Benchmark
    open fun mathBenchmark() = 3.14
}
