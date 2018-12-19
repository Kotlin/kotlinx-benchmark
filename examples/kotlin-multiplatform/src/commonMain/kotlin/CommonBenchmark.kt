package test

import org.jetbrains.gradle.benchmarks.*
import kotlin.math.*

@State(Scope.Benchmark)
class CommonBenchmark {
    private var data = 0.0

    @Setup
    fun setUp() {
        data = 3.0
    }

    @Benchmark
    fun mathBenchmark(): Double {
        return log(sqrt(data) * cos(data), 2.0)
    }

}
