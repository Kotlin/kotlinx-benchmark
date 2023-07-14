package kotlinx.benchmark.integration

class BenchmarkConfiguration {
    var iterations: Int? = null
    var warmups: Int? = null
    var iterationTime: Long? = null
    var iterationTimeUnit: String? = null
    var mode: String? = null
    var outputTimeUnit: String? = null
    var reportFormat: String? = null
    var includes: MutableList<String> = mutableListOf()
    var excludes: MutableList<String> = mutableListOf()
    var params: MutableMap<String, MutableList<Any>> = mutableMapOf()
    var advanced: MutableMap<String, Any> = mutableMapOf()

    fun include(pattern: String) {
        includes.add(pattern)
    }

    fun exclude(pattern: String) {
        excludes.add(pattern)
    }

    fun param(name: String, vararg value: Any) {
        val values = params.getOrPut(name) { mutableListOf() }
        values.addAll(value)
    }

    fun advanced(name: String, value: Any) {
        advanced[name] = value
    }

    fun lines(name: String): List<String> = """
        $name {
            iterations = $iterations
            warmups = $warmups
            iterationTime = $iterationTime
            iterationTimeUnit = ${iterationTimeUnit?.escape()}
            mode = ${mode?.escape()}
            outputTimeUnit = ${outputTimeUnit?.escape()}
            reportFormat = ${reportFormat?.escape()}
            includes = ${includes.map { it.escape() }.joinToString(prefix = "[", postfix = "]")}
            excludes = ${excludes.map { it.escape() }.joinToString(prefix = "[", postfix = "]")}
            params = ${params.map { "\"${it.key}\" to listOf(${it.value.joinToString()})" }.joinToString(prefix = "mapOf(", postfix = ")")}
            advanced = ${advanced.map { "\"${it.key}\" to \"${it.value}\"" }.joinToString(prefix = "mapOf(", postfix = ")")}
        }
    """.trimIndent().split("\n")
}

private fun String.escape(): String = "\"$this\""
