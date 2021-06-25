package kotlinx.benchmark.integration

class BenchmarkConfiguration {
    var iterations: Int? = null
    var warmups: Int? = null
    var iterationTime: Long? = null
    var iterationTimeUnit: String? = null
    var mode: String? = null
    var outputTimeUnit: String? = null
    var reportFormat: String? = null

    fun lines(name: String): List<String> = """
        $name {
            iterations = $iterations
            warmups = $warmups
            iterationTime = $iterationTime
            iterationTimeUnit = ${iterationTimeUnit?.escape()}
            mode = ${mode?.escape()}
            outputTimeUnit = ${outputTimeUnit?.escape()}
            reportFormat = ${reportFormat?.escape()}
        }
    """.trimIndent().split("\n")
}

private fun String.escape(): String = "\"$this\""
