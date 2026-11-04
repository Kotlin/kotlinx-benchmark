package kotlinx.benchmark.tests

import kotlinx.benchmark.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class AggregatedResultsFormattingTest {
    private val singleThreadedResults = AggregateIterationResult(arrayOf(IterationResult(1.seconds, 100)))
    private val multiThreadedResults = AggregateIterationResult(
        arrayOf(
            IterationResult(1.seconds, 100),
            IterationResult(1.seconds, 200),
            IterationResult(1.seconds, 100),
            IterationResult(1.seconds, 100)
        )
    )
    private val unevenTimeDistribution = AggregateIterationResult(
        arrayOf(
            IterationResult(1.seconds, 100),
            IterationResult(2.seconds, 200),
        )
    )

    @Test
    fun nanosToText() {
        assertEquals(
            "100.0000 ops/sec", singleThreadedResults.nanosToText(
                Mode.Throughput, BenchmarkTimeUnit.SECONDS
            )
        )
        assertEquals(
            "0.1000000 ops/ms", singleThreadedResults.nanosToText(
                Mode.Throughput, BenchmarkTimeUnit.MILLISECONDS
            )
        )
        // Note that for multiple threads throughput results are accumulated
        assertEquals(
            "500.000 ops/sec", multiThreadedResults.nanosToText(
                Mode.Throughput, BenchmarkTimeUnit.SECONDS
            )
        )
        assertEquals(
            "0.500000 ops/ms", multiThreadedResults.nanosToText(
                Mode.Throughput, BenchmarkTimeUnit.MILLISECONDS
            )
        )

        assertEquals(
            "0.01000000 sec/op", singleThreadedResults.nanosToText(
                Mode.AverageTime, BenchmarkTimeUnit.SECONDS
            )
        )
        assertEquals(
            "10.00000 ms/op", singleThreadedResults.nanosToText(
                Mode.AverageTime, BenchmarkTimeUnit.MILLISECONDS
            )
        )
        // In avg time mode, results are averaged for each thread, and then along all threads,
        // meaning that it is not just reciprocal of throughput.
        assertEquals(
            "0.00875000 sec/op", multiThreadedResults.nanosToText(
                Mode.AverageTime, BenchmarkTimeUnit.SECONDS
            )
        )
        assertEquals(
            "8.75000 ms/op", multiThreadedResults.nanosToText(
                Mode.AverageTime, BenchmarkTimeUnit.MILLISECONDS
            )
        )

        assertEquals(
            "200.000 ops/sec",
            unevenTimeDistribution.nanosToText(Mode.Throughput, BenchmarkTimeUnit.SECONDS),
        )
        assertEquals(
            "0.01000000 sec/op", unevenTimeDistribution.nanosToText(
                Mode.AverageTime, BenchmarkTimeUnit.SECONDS
            )
        )
    }
}
