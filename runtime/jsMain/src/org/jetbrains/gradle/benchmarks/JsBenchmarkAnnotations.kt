package org.jetbrains.gradle.benchmarks

actual enum class Scope {
    Benchmark
}

actual annotation class State(actual val value: Scope)
actual annotation class Setup
actual annotation class Benchmark