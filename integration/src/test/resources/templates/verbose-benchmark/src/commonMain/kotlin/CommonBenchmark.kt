package test

import kotlinx.benchmark.*
import java.io.*

@State(Scope.Benchmark)
@Measurement(iterations = 1, time = 1, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@Warmup(iterations = 1, time = 1, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
open class CommonBenchmark {

    @Setup
    fun setup() {
        FileOutputStream(FileDescriptor.err).use {
            it.write("Hello, stderr!\n".toByteArray())
        }
    }
    @Benchmark
    fun bm() = "hello"
}
