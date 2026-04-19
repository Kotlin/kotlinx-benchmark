package test

import kotlinx.benchmark.*
import kotlin.math.*

@State(Scope.Benchmark)
@Measurement(iterations = 1, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@Warmup(iterations = 2, time = 500, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
open class CommonBenchmark {

    var data1: Int = 0

    var data2: String = ""

    fun function1() {
        // println("function1")
    }

    fun function2() {
        // println("function2")
    }
}
