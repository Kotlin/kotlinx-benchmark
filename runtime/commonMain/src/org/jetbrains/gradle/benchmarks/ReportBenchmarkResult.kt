package org.jetbrains.gradle.benchmarks

class ReportBenchmarkResult(
    val benchmark: String,
    val score: Double,
    val error: Double,
    val confidence: Pair<Double, Double>,
    val percentiles: Map<Double, Double>,
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
       "scoreError": ${it.error},
       "scoreConfidence" : [
          ${it.confidence.first},
          ${it.confidence.second}
       ],
       "scorePercentiles" : {
          ${it.percentiles.entries.joinToString(separator = ",\n          ") {
        "\"${it.key.format(2)}\" : ${it.value}"
    }}
       },
       "scoreUnit" : "ops/s",
       "rawData" : [
           ${it.values.joinToString(
        prefix = "[\n             ",
        postfix = "\n           ]",
        separator = ",\n             "
    )}
       ]
    },
    "secondaryMetrics" : {
    }
  }"""
}
