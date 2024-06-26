import kotlinx.benchmark.*
import kotlin.math.*

@State(Scope.Benchmark)
@Measurement(iterations = 3, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
open class RootBenchmark {
    @Benchmark
    open fun mathBenchmark(): Double {
        return log(sqrt(3.0) * cos(3.0), 2.0)
    }
}
