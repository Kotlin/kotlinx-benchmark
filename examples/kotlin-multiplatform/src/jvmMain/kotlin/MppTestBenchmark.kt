package test

import org.openjdk.jmh.annotations.*
import java.util.concurrent.*

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 0)
@Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
class MppTestBenchmark {
    private var data = 0.0

    @Setup
    fun setUp() {
        data = 3.0
    }

    @Benchmark
    fun sqrtBenchmark(): Double {
        return Math.sqrt(data)
    }

    @Benchmark
    fun cosBenchmark(): Double {
        return Math.cos(data)
    }
}

actual typealias Scope = org.openjdk.jmh.annotations.Scope
actual typealias State = org.openjdk.jmh.annotations.State
actual typealias Setup = org.openjdk.jmh.annotations.Setup
actual typealias Benchmark = org.openjdk.jmh.annotations.Benchmark