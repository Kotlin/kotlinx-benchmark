package org.jetbrains.gradle.benchmarks.native

import org.jetbrains.gradle.benchmarks.*
import platform.posix.*
import kotlin.system.*

class Suite(private val args: Array<out String>) {
    private class BenchmarkDescriptor(val name: String, val function: () -> Unit)

    private val benchmarks = mutableListOf<BenchmarkDescriptor>()
    private val reportFile = args.first()

    var iterations = 1_000_000

    fun add(name: String, function: () -> Unit) {
        benchmarks.add(BenchmarkDescriptor(name, function))
    }

    fun run() {
        val results = benchmarks.map { benchmark ->
            println(benchmark.name)
            val time = measureNanoTime {
                repeat(iterations) {
                    benchmark.function()
                }
            }
            val nanosecondsPerOperation = time.toDouble() / iterations
            val operationsPerSecond = 1_000_000_000.0 / nanosecondsPerOperation
            println("  ~ $operationsPerSecond ops/sec")
            println()
            ReportBenchmarkResult(benchmark.name, operationsPerSecond)
        }

        val file = fopen(reportFile, "w")
        fputs(results.toJson(), file)
        fclose(file)
    }
}

