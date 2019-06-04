package test

import org.jetbrains.gradle.benchmarks.*
import kotlin.math.*

@State(Scope.Benchmark)
@Measurement(iterations = 3, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
class CommonBenchmark {
    private var data = 0.0
    private lateinit var text : String 
    
    @Setup
    fun setUp() {
        data = 3.0
        text = "Hello!"
        // println("Setup!")
    }
    
    @TearDown
    fun teardown() {
        // println("Teardown!")
    }

    @Benchmark
    fun exception() {
        try {
            fail()   
        } catch (e: Throwable) {
            throw Exception("I failed!", e)
        }
    }

    private fun fail() {
        TODO("not implemented") 
    }

    @Benchmark
    fun mathBenchmark(): Double {
        return log(sqrt(data) * cos(data), 2.0)
    }

    @Benchmark
    fun longBenchmark(): Double {
        var value = 1.0
        repeat(1000) {
            value *= text.length
        }
        return value
    }

}
