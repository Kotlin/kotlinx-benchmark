package kotlinx.benchmark

expect fun saveReport(reportFile: String?, results: Collection<ReportBenchmarkResult>)

fun formatJson(results: Collection<ReportBenchmarkResult>) =
    results.joinToString(",", prefix = "[", postfix = "\n]") { result ->
        """
  {
    "benchmark" : "${result.benchmark.name}",
    "mode" : "${result.config.mode.toText()}",
    "warmupIterations" : ${result.config.warmups},
    "warmupTime" : "${result.config.iterationTime} ${result.config.iterationTimeUnit.toText()}",
    "measurementIterations" : ${result.config.iterations},
    "measurementTime" : "${result.config.iterationTime} ${result.config.iterationTimeUnit.toText()}",
    "params" : {
          ${result.params.entries.joinToString(separator = ",\n          ") {
            "\"${it.key}\" : \"${it.value}\""
        }}
    },
    "primaryMetric" : {
       "score": ${result.score},
       "scoreError": ${result.error},
       "scoreConfidence" : [
          ${result.confidence.first},
          ${result.confidence.second}
       ],
       "scorePercentiles" : {
          ${result.percentiles.entries.joinToString(separator = ",\n          ") {
            "\"${it.key.format(2)}\" : ${it.value}"
        }}
       },
       "scoreUnit" : "${unitText(result.config.mode, result.config.outputTimeUnit)}",
       "rawData" : [
           ${result.values.joinToString(
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
