package kotlinx.benchmark.integration

enum class JsBenchmarksExecutor {
    BenchmarkJs,
    BuiltIn
}

class BenchmarkTarget {
    var jmhVersion: String? = null
    var jsBenchmarksExecutor: JsBenchmarksExecutor? = null

    fun lines(name: String): List<String> = """
        register("$name") {
            ${jmhVersion?.let { "jmhVersion = \"$it\"" } ?: ""}
            ${jsBenchmarksExecutor?.let { "jsBenchmarksExecutor = JsBenchmarksExecutor.${it.name}" } ?: ""}
        }
    """.trimIndent().split("\n")
}