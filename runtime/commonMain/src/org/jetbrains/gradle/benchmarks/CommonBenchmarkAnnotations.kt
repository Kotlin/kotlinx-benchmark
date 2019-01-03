package org.jetbrains.gradle.benchmarks

@Target(AnnotationTarget.FUNCTION)
expect annotation class Setup()

@Target(AnnotationTarget.FUNCTION)
expect annotation class Benchmark()

@Target(AnnotationTarget.CLASS)
expect annotation class State(val value: Scope)

expect enum class Scope {
    Benchmark
}
