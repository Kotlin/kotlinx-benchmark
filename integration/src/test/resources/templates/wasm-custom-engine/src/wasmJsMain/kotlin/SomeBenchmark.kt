package test

import kotlinx.benchmark.*
import kotlin.math.*

@State(Scope.Benchmark)
@Measurement(iterations = 1, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
class SomeBenchmark {
    @Benchmark
    fun mathBenchmark(): Double {
        return log(sqrt(3.0) * cos(3.0), 2.0)
    }

    // Use coroutines API, so that stack switching intructions aren't optimized away.
    @Benchmark
    fun sequenceBenchmark(): Int {
        return sequence {
            for (i in 0 until 10) yield(i)
        }.sum()
    }
}