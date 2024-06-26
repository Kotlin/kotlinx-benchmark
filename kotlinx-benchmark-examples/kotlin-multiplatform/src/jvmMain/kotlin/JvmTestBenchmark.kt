package test

import org.openjdk.jmh.annotations.*
import java.util.concurrent.*

const val WARMUP_ITERATIONS = 20
@State(Scope.Benchmark)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = WARMUP_ITERATIONS, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
class JvmTestBenchmark {
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

