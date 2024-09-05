package test

import org.openjdk.jmh.annotations.*
import java.util.concurrent.*
import kotlin.math.*

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 0)
@Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
class TestBenchmark {
    private var data: TestData = TestData(0.0)

    @Setup
    fun setUp() {
        data = TestData(3.0)
    }

    @Benchmark
    fun sqrtBenchmark(): Double {
        return sqrt(data.value)
    }

    @Benchmark
    fun cosBenchmark(): Double {
        return cos(data.value)
    }
}