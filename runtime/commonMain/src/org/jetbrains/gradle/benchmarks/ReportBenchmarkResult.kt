package org.jetbrains.gradle.benchmarks

data class ReportBenchmarkResult(val benchmark: String, val score: Double)

fun List<ReportBenchmarkResult>.toJson() = joinToString(",", prefix = "[", postfix = "\n]") {
    """
  {
    "benchmark": "${it.benchmark}",
    "score": ${it.score}
  }"""
}
