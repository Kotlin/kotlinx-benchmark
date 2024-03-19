package test

import kotlinx.benchmark.*
import kotlin.math.*

@State(Scope.Benchmark)
@Measurement(iterations = 3, time = 1, timeUnit = BenchmarkTimeUnit.NANOSECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.Throughput)
open class CommonBenchmark {

    var data = 0

    val valProperty: String = "test"

    final var finalProperty = "This property is final"

    private var privateProperty = "This property is private"

    internal var internalProperty = "This property is internal"

    @Benchmark
    open fun mathBenchmark(): Double {
        return log(sqrt(3.0) * cos(3.0), 2.0)
    }

    fun setUpMethod() {
        // println("Setup!")
    }

    fun teardownMethod() {
        // println("Teardown!")
    }

    private fun privateMethod() {
        // println("Private method!")
    }

    internal fun internalMethod() {
        // println("Internal method!")
    }

    protected open fun protectedMethod() {
        // println("Protected method!")
    }

    final fun finalMethod() {
        // println("Final method!")
    }

    fun plainMethod() {
        // println("Plain method!")
    }

    fun blackholeFunction(bh: Blackhole) {
        val result = sqrt(3.0)
        bh.consume(result)
    }

    fun stateFunction(state: CommonBenchmark) {
        val result = sqrt(3.0)
        state.data = result.toInt()
    }
    
    fun methodWithArg(number: Int): Double {
        return sqrt(number.toDouble())
    }
}