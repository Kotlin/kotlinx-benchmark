package org.jetbrains.gradle.benchmarks.native

import kotlin.system.*

class Suite {
    private val benchmarks = mutableListOf<BenchmarkDescriptor>()
    var iterations = 100_000

    fun add(name: String, function: () -> Unit) {
        benchmarks.add(BenchmarkDescriptor(name, function))
    }

    fun run() {
        val results = benchmarks.map { benchmark ->
            val time = measureNanoTime {
                repeat(iterations) {
                    benchmark.function()
                }
            }
            val nanosecondsPerOperation = time.toDouble() / iterations
            BenchmarkResult(benchmark.name, BenchmarkMetric(1_000_000_000.0 / nanosecondsPerOperation))
        }

        val json = results.joinToString(",", prefix = "[", postfix = "]") {
            """
  {
    "benchmark": "${it.benchmark}",
    "primaryMetric": {
      "score": ${it.primaryMetric.score}
    }
  }"""
        }
        println(json)
    }
}

class BenchmarkDescriptor(val name: String, val function: () -> Unit)
data class BenchmarkResult(val benchmark: String, val primaryMetric: BenchmarkMetric)
data class BenchmarkMetric(val score: Double)
