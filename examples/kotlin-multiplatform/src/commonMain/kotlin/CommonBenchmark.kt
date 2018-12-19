package test

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

@Target(AnnotationTarget.FUNCTION)
expect annotation class Setup()

@Target(AnnotationTarget.FUNCTION)
expect annotation class Benchmark()

@Target(AnnotationTarget.CLASS)
expect annotation class State(val value: Scope)

expect public enum class Scope {
    Benchmark
}