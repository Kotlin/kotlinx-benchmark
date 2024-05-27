package test

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sqrt

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 0)
@Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
abstract class BaseBenchmark {

    protected lateinit var data: TestData

    @Setup
    fun setUp() {
        data = TestData(50.0)
    }
}

class TestInheritedBenchmark: BaseBenchmark() {
    @Benchmark
    fun sqrtBenchmark(): Double {
        return sqrt(data.value)
    }

    @Benchmark
    fun cosBenchmark(): Double {
        return cos(data.value)
    }
}


