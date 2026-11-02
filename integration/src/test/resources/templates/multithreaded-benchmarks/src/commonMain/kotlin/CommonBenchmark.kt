package test

import kotlinx.benchmark.*
import kotlin.math.*

@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@Measurement(iterations = 1, time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@Threads(2)
open class CommonBenchmark {
    @Benchmark
    open fun mathBenchmark(): Double {
        return log(sqrt(3.0) * cos(3.0), 2.0)
    }
}
