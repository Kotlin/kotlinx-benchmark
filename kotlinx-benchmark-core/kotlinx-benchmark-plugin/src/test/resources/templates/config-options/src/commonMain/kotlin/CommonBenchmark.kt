package test

import kotlinx.benchmark.*
import kotlin.math.*

@State(Scope.Benchmark)
@Measurement(iterations = 3, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@Warmup(iterations = 5, time = 500, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
open class CommonBenchmark {

    @Param("3.0")
    var data: Double = 0.0

    @Benchmark
    fun mathBenchmark(): Double {
        return log(sqrt(data) * cos(data), 2.0)
    }
}
