package test

import kotlinx.benchmark.*

@State(Scope.Benchmark)
@Measurement(iterations = 1, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@Warmup(iterations = 1, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
open class CommonBenchmark {
    @Benchmark
    open fun mathBenchmark(): Double {
        return valuableWorkload()
    }
}
