package test

import kotlinx.benchmark.*
import kotlin.math.*

@State(Scope.Benchmark)
@Measurement(iterations = 3, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
class ParamBenchmark {
    
    @Param("1", "2")
    var data = 0

    @Param("1", "2")
    var value = 0
    
    private lateinit var text : String 
    
    @Setup
    fun setUp() {
        text = "Hello!"
    }
    
    @Benchmark
    fun mathBenchmark(): Double {
        return log(sqrt(data.toDouble()) * data, 2.0)
    }
    
    @Benchmark
    fun otherBenchmark(): Int {
        return data + data
    }
}
