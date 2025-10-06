package test

import kotlinx.benchmark.*
import kotlin.math.*

@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@Measurement(iterations = 1, time = 200, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
open class JvmCustomBenchmark {
    @Benchmark
    open fun hashCodeBenchmark(): Int {
        val value = log(sqrt(3.0) * cos(3.0), 2.0)
        return JvmTestData(CommonTestData(value)).hashCode()
    }
}
