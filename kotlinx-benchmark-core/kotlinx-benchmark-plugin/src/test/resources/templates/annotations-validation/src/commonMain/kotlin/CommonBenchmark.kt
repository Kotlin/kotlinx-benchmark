package test

import kotlinx.benchmark.*
import kotlin.math.*

@State(Scope.Benchmark)
@Measurement(iterations = 3, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@Warmup(iterations = 5, time = 500, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
open class CommonBenchmark {

    var varProperty: String = "var"

    val valProperty: String = "val"

    private var privateProperty: String = "private"

    internal var internalProperty: String = "internal"

    protected var protectedProperty: String = "protected"

    final var finalProperty: String = "final"

    var nullableStringProperty: String? = null

    var nullableIntProperty: Int? = null

    var nullableUIntProperty: UInt? = null

    var notSupportedTypeProperty: Regex = Regex("notSupportedType")

    fun plainFunction() {
        // println("Plain function!")
    }

    private fun privateFunction() {
        // println("Private function!")
    }

    internal fun internalFunction() {
        // println("Internal function!")
    }

    protected open fun protectedFunction() {
        // println("Protected function!")
    }

    final fun finalFunction() {
        // println("Final function!")
    }

    fun functionWithBlackholeArgument(bh: Blackhole) {
        val result = sqrt(3.0)
        bh.consume(result)
    }

    fun functionWithTwoBlackholeArguments(bh1: Blackhole, bh2: Blackhole) {
        val result = sqrt(3.0)
        bh1.consume(result)
        bh2.consume(result)
    }

    fun functionWithIntArgument(number: Int): Double {
        return sqrt(number.toDouble())
    }
}
