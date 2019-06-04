package org.jetbrains.gradle.benchmarks

expect fun saveReport(reportFile: String?, results: Collection<ReportBenchmarkResult>)

fun formatJson(results: Collection<ReportBenchmarkResult>) =
    results.joinToString(",", prefix = "[", postfix = "\n]") {
        """
  {
    "benchmark" : "${it.benchmark.name}",
    "mode" : "${it.config.mode.toText()}",
    "warmupIterations" : ${it.config.warmups},
    "warmupTime" : "${it.config.iterationTime} ${it.config.iterationTimeUnit.toText()}",
    "measurementIterations" : ${it.config.iterations},
    "measurementTime" : "${it.config.iterationTime} ${it.config.iterationTimeUnit.toText()}",
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
       "scoreUnit" : "${unitText(it.config.mode, it.config.outputTimeUnit)}",
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
