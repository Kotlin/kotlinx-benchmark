package kotlinx.benchmark.integration

class BenchmarkConfiguration {
    var iterations: Int? = null
    var warmups: Int? = null
    var iterationTime: Long? = null
    var iterationTimeUnit: String? = null
    var mode: String? = null
    var outputTimeUnit: String? = null
    var reportFormat: String? = null
    private var includes: MutableList<String> = mutableListOf()
    private var excludes: MutableList<String> = mutableListOf()
    private var params: MutableMap<String, MutableList<Any>> = mutableMapOf()
    private var advanced: MutableMap<String, Any> = mutableMapOf()

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
            includes = ${includes.map { it.escape() }}
            excludes = ${excludes.map { it.escape() }}
            ${params.entries.joinToString(separator = "\n") { """param("${it.key}", ${it.value.joinToString()})""" }}
            ${advanced.entries.joinToString(separator = "\n") {
                """advanced("${it.key}", ${if (it.value is String) "\"${it.value}\"" else it.value})"""
            }}
        }
    """.trimIndent().split("\n")
}

private fun String.escape(): String = "\"$this\""