package org.jetbrains.gradle.benchmarks.js

import kotlinx.cli.*
import org.jetbrains.gradle.benchmarks.*
import kotlin.js.*
import kotlin.math.*

external fun require(module: String): dynamic
private val benchmarkJs: dynamic = require("benchmark")
private val fs = require("fs")
private val process = require("process")

class Suite(val title: String, @Suppress("UNUSED_PARAMETER") dummy_args: Array<out String>) {
    private val args = RunnerCommandLine().also { it.parse((process["argv"] as Array<String>).drop(2)) }
    private val suite: dynamic = benchmarkJs.Suite()
    val results = mutableListOf<ReportBenchmarkResult>()

    fun run() {
        suite.on("complete") {
            when (args.traceFormat) {
                "xml" -> {
                    println(ijLogFinish(title, ""))
                }
                "text" -> {
                    println()
                }
                else -> throw UnsupportedOperationException("Format ${args.traceFormat} is not supported.")
            }
            fs.writeFile(args.reportFile, results.toJson()) { err -> if (err != null) throw err }
        }

        when (args.traceFormat) {
            "xml" -> {
                println(ijLogStart(title, ""))
            }
            "text" -> {
            }
            else -> throw UnsupportedOperationException("Format ${args.traceFormat} is not supported.")
        }

        suite.run()
    }

    fun add(name: String, function: () -> Promise<*>, setup: () -> Unit, teardown: () -> Unit) {
        suite.add(name) { deferred: Promise<Unit> ->
            // Mind asDynamic: this is **not** a regular promise
            function().then { (deferred.asDynamic()).resolve() }
        }

        configureBenchmark(setup, teardown, asynchronous = true)
    }

    fun add(name: String, function: () -> Any?, setup: () -> Unit, teardown: () -> Unit) {
        suite.add(name, function)
        configureBenchmark(setup, teardown, asynchronous = false)
    }

    private fun configureBenchmark(setup: () -> Unit, teardown: () -> Unit, asynchronous: Boolean) {
        val benchmark = suite[suite.length - 1] // take back last added benchmark and subscribe to events

        // TODO: Configure properly
        // initCount: The default number of times to execute a test on a benchmark’s first cycle
        // minTime: The time needed to reduce the percent uncertainty of measurement to 1% (secs).
        // maxTime: The maximum time a benchmark is allowed to run before finishing (secs).

        benchmark.options.minTime = args.iterationTime / 1000.0
        benchmark.options.maxTime = args.iterationTime * args.iterations / 1000.0
        benchmark.options.async = asynchronous
        benchmark.options.defer = asynchronous

        benchmark.on("start") { event ->
            val benchmarkFQN = event.target.name
            when (args.traceFormat) {
                "xml" -> {
                    println(ijLogStart(benchmarkFQN, title))
                }
                "text" -> {
                    println()
                    println("… $benchmarkFQN")
                }
                else -> throw UnsupportedOperationException("Format ${args.traceFormat} is not supported.")
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

            when (args.traceFormat) {
                "xml" -> {
                    println(ijLogFinish(benchmarkFQN, title))
                    println(ijLogOutput(benchmarkFQN, title, message))
                }
                "text" -> {
                    println("  $message")
                }
                else -> throw UnsupportedOperationException("Format ${args.traceFormat} is not supported.")
            }

            results.add(result)
        }
    }
}

