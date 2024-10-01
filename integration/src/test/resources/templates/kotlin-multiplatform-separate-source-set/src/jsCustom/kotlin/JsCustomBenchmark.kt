package test

import kotlinx.benchmark.*
import kotlin.math.*

@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 200, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
open class JsCustomBenchmark {
    @Benchmark
    open fun hashCodeBenchmark(): Int {
        val value = log(sqrt(3.0) * cos(3.0), 2.0)
        return JsTestData(CommonTestData(value)).hashCode()
    }
}