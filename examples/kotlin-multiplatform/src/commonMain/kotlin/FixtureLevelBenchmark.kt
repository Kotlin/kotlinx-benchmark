package test

import kotlinx.benchmark.*
import kotlin.math.*

@State(Scope.Benchmark)
@Measurement(iterations = 3, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
class FixtureLevelBenchmark {
    private var data: Double = 0.0

    private var iterations: Int = 0

    @Setup(Level.Trial)
    fun trialSetup() {
        data = 3.0
        iterations = 0
    }


    @Setup(Level.Iteration)
    fun iterationSetup() {
        iterations++
    }

    @TearDown(Level.Trial)
    fun trialTearDown() {
//        println("Total iterations = $iterations")
    }

    @Benchmark
    fun mathBenchmark(): Double {
        return log(sqrt(data) * cos(data), 2.0)
    }
}