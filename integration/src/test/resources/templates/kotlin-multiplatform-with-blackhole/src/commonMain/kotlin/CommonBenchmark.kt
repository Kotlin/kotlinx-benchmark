package test

import kotlinx.benchmark.*
import kotlin.math.*

@State(Scope.Benchmark)
@Measurement(iterations = 1, time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
open class CommonBenchmark {
    @Benchmark
    open fun mathBenchmark(bh: Blackhole) {
        bh.consume(log(sqrt(3.0) * cos(3.0), 2.0))
    }
}
