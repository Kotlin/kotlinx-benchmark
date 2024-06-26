package test

import org.openjdk.jmh.annotations.*
import java.util.concurrent.*

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 0)
@Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
class KtsTestBenchmark {
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