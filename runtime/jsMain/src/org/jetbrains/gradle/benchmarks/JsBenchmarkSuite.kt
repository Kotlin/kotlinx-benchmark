package org.jetbrains.gradle.benchmarks.js

import org.jetbrains.gradle.benchmarks.*

external fun require(module: String): dynamic
private val benchmarkJs: dynamic = require("benchmark")
private val fs = require("fs");
private val process = require("process");

class Suite(dummy_args: Array<out String>) {
    private val args = (process["argv"] as Array<String>).drop(2)
    private val reportFile = args.first()
    private val suite: dynamic = benchmarkJs.Suite()
    val results = mutableListOf<ReportBenchmarkResult>()

    fun run() {
        suite.on("complete") {
            println()
        }
        suite.run()
        fs.writeFile(reportFile, results.toJson()) { err -> if (err) throw err }
    }

    fun add(name: String, function: () -> Any?, setup: () -> Unit, teardown: () -> Unit) {
        suite.add(name, function)
        val benchmark = suite[suite.length - 1] // take back last added benchmark and subscribe to events
        benchmark.on("start") { event ->
            println()
            println("… ${event.target.name}")
            setup()
        }
        benchmark.on("complete") { event ->
            teardown()
            val stats = event.target.stats
            val samples = stats.sample.unsafeCast<DoubleArray>().map { 1 / it }.toDoubleArray()
            results.add(ReportBenchmarksStatistics.createResult(event.target.name, samples))
        }
    }
}

