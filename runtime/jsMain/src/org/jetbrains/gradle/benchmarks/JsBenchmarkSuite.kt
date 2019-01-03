package org.jetbrains.gradle.benchmarks.js

fun suiteJson(suite: dynamic): String {
    val list = mutableListOf<BenchmarkResult>()
    for (index in 0 until suite.length) {
        val benchmark = suite[index]
        list.add(BenchmarkResult(benchmark.name, BenchmarkMetric(benchmark.hz)))
    }
    return JSON.stringify(list)
}

fun benchmarkJson(benchmark: dynamic): String {
    val result = BenchmarkResult(benchmark.name, BenchmarkMetric(benchmark.hz))
    return JSON.stringify(result)
}

data class BenchmarkResult(val benchmark: String, val primaryMetric: BenchmarkMetric)
data class BenchmarkMetric(val score: Double)
