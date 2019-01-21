package org.jetbrains.gradle.benchmarks

class ReportBenchmarkResult(
    val benchmark: String,
    val score: Double,
    val confidence: Pair<Double, Double>,
    val values: DoubleArray
)

fun List<ReportBenchmarkResult>.toJson() = joinToString(",", prefix = "[", postfix = "\n]") {
    """
  {
    "benchmark" : "${it.benchmark}",
    "mode" : "thrpt",
    "warmupIterations" : 1,
    "warmupTime" : "500 ms",
    "measurementIterations" : 1,
    "measurementTime" : "500 ms",
    "primaryMetric" : {
       "score": ${it.score},
       "scoreError": ${(it.confidence.second - it.confidence.first) / 2},
       "scoreConfidence" : [
          ${it.confidence.first},
          ${it.confidence.second}
       ],
       "scoreUnit" : "ops/s",
       "rawData" : [
           ${it.values.joinToString(prefix = "[\n             ", postfix = "\n           ]", separator = ",\n             ")}
       ]
    },
    "secondaryMetrics" : {
    }
  }"""
}
