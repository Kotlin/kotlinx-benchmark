package org.jetbrains.gradle.benchmarks.js

import org.jetbrains.gradle.benchmarks.*
import kotlin.math.*

external fun require(module: String): dynamic
private val benchmarkJs: dynamic = require("benchmark")
private val fs = require("fs");
private val process = require("process");

class Suite(val title: String, @Suppress("UNUSED_PARAMETER") dummy_args: Array<out String>) {
    private val args = (process["argv"] as Array<String>).drop(2)
    private val iterations = args[1].toInt()
    private val iterationTime = args[2].toInt()
    private val format = args[3]
    private val reportFile = args.first()
    private val suite: dynamic = benchmarkJs.Suite()
    val results = mutableListOf<ReportBenchmarkResult>()

    fun run() {
        suite.on("complete") {
            when (format) {
                "xml" -> {
                    println(ijLogFinish(title, ""))
                }
                "text" -> {
                    println()
                }
                else -> throw UnsupportedOperationException("Format $format is not supported.")
            }
        }
        when (format) {
            "xml" -> {
                println(ijLogStart(title, ""))
            }
            "text" -> {}
            else -> throw UnsupportedOperationException("Format $format is not supported.")
        }
        suite.run()
        fs.writeFile(reportFile, results.toJson()) { err -> if (err) throw err }
    }

    fun add(name: String, function: () -> Any?, setup: () -> Unit, teardown: () -> Unit) {
        suite.add(name, function)
        val benchmark = suite[suite.length - 1] // take back last added benchmark and subscribe to events
        
        // TODO: Configure properly
        // initCount: The default number of times to execute a test on a benchmark’s first cycle
        // minTime: The time needed to reduce the percent uncertainty of measurement to 1% (secs).
        // maxTime: The maximum time a benchmark is allowed to run before finishing (secs).
        
        benchmark.options.minTime = iterationTime / 1000.0
        benchmark.options.maxTime = iterationTime * iterations / 1000.0

        benchmark.on("start") { event ->
            val benchmarkFQN = event.target.name
            when (format) {
                "xml" -> {
                    println(ijLogStart(benchmarkFQN, title))
                }
                "text" -> {
                    println()
                    println("… $benchmarkFQN")
                }
                else -> throw UnsupportedOperationException("Format $format is not supported.")
            }

            setup()
        }
        benchmark.on("complete") { event ->
            teardown()
            val benchmarkFQN = event.target.name
            val stats = event.target.stats
            val samples = stats.sample.unsafeCast<DoubleArray>().map { 1 / it }.toDoubleArray()
            val result = ReportBenchmarksStatistics.createResult(event.target.name, samples)
            val message = with(result) {
                val d = (4 - log10(score).toInt()).coerceAtLeast(0) // display 4 significant digits
                "  ~ ${score.format(d)} ops/sec ±${(error / score * 100).format(2)}%"
            }

            when (format) {
                "xml" -> {
                    println(ijLogFinish(benchmarkFQN, title))
                    println(ijLogOutput(benchmarkFQN, title, message))
                }
                "text" -> {
                    println("  $message")
                }
                else -> throw UnsupportedOperationException("Format $format is not supported.")
            }


            results.add(result)
        }
    }
}

