package test

import kotlin.math.*

@State(Scope.Benchmark)
class JsTestBenchmark {
    private var data = 0.0

    @Setup
    fun setUp() {
        data = 3.0
    }

    @Benchmark
    fun sqrtBenchmark(): Double {
        return sqrt(data)
    }

    @Benchmark
    fun cosBenchmark(): Double {
        return cos(data)
    }
}

actual public enum class Scope {
    Benchmark
}

actual annotation class State(actual val value: Scope)
actual annotation class Setup
actual annotation class Benchmark
