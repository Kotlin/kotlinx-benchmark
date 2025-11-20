package test

import kotlinx.benchmark.*
import org.openjdk.jmh.annotations.Fork
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
open class InverseSquareRootBenchmark {
    private var data = Random.nextFloat()

    @Benchmark
    fun invSqrtBaseline(): Float {
        return 1.0f / sqrt(data)
    }

    @Benchmark
    fun invSqrtOptimized(): Float {
        return fastInvSqrt(data)
    }
}

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
open class PowerBenchmark {
    @Param("2.71")
    var power: Double = 0.0

    @Param("-3.0", "421431.243214")
    var value: Double = 0.0

    @Benchmark
    fun powerBaseline(): Double = value.pow(power)

    @Benchmark
    fun powerOptimized(): Double = fastPower(value, power)
}

// See https://en.wikipedia.org/wiki/Fast_inverse_square_root
private fun fastInvSqrt(x: Float): Float {
    val y = Float.fromBits(0x5f3759df - (x.toBits() shr 1))
    return y * (1.5F - (x * 0.5F * y * y))
}

// Credits: https://martin.ankerl.com/2007/10/04/optimized-pow-approximation-for-java-and-c-c/
private fun fastPower(a: Double, b: Double): Double {
    val tmp = a.toBits()
    val tmp2 = (b * (tmp - 4606921280493453312L)).toLong() + 4606921280493453312L
    return Double.fromBits(tmp2)
}
