package test

import kotlinx.benchmark.*
import kotlin.math.*

@State(Scope.Benchmark)
@OutputTimeUnit(BenchmarkTimeUnit.SECONDS)
abstract class BaseBenchmark {
    protected var data = 0.0
    private lateinit var text: String

    @Setup
    fun baseSetup() {
        data = 3.0
        text = "Hello!"
    }

    @TearDown
    fun baseTeardown() {
    }

    @Benchmark
    fun baseBenchmark(): Double {
        var value = 1.0
        repeat(1000) {
            value *= text.length
        }
        return value
    }
}

@State(Scope.Benchmark)
@Measurement(iterations = 10)
class InheritedBenchmark : BaseBenchmark() {
    @Setup
    fun inheritedSetup() {
    }

    @TearDown
    fun inheritedTeardown() {
    }

    @Benchmark
    fun inheritedBenchmark(): Double {
        return log(sqrt(data) * cos(data), 2.0)
    }
}
