package kotlinx.benchmark

import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi

@KotlinxBenchmarkRuntimeInternalApi
abstract class SuiteExecutor(
    val executionName: String,
    configPath: String,
    xmlReporter: (() -> BenchmarkProgress)? = null
) {
    private val config = RunnerConfiguration(configPath.readFile())

    val reporter = BenchmarkProgress.create(config.traceFormat, xmlReporter)

    private val reportFormatter = BenchmarkReportFormatter.create(config.reportFormat)

    private val results = mutableListOf<ReportBenchmarkResult>()

    private val suites = mutableListOf<SuiteDescriptor<*>>()

    fun <T> suite(descriptor: SuiteDescriptor<T>) {
        suites.add(descriptor)
    }

    fun run() {
        //println(config.toString())
        val include = if (config.include.isEmpty())
            listOf(Regex(".*"))
        else
            config.include.map { Regex(it) }
        val exclude = config.exclude.map { Regex(it) }

        @Suppress("UNCHECKED_CAST")
        val benchmarks = suites.flatMap { suite ->
            suite.benchmarks
                .filter { benchmark ->
                    val fullName = suite.name + "." + benchmark.name
                    include.any { it.containsMatchIn(fullName) } && exclude.none { it.containsMatchIn(fullName) }
                } as List<BenchmarkDescriptor<Any?>>
        }

        run(config, benchmarks, { reporter.startSuite(executionName) }) {
            val summary = TextBenchmarkReportFormatter.format(results)
            reporter.endSuite(executionName, summary)
            config.reportFile.writeFile(reportFormatter.format(results))
        }
    }

    fun result(result: ReportBenchmarkResult) {
        results.add(result)
    }

    abstract fun run(
        runnerConfiguration: RunnerConfiguration,
        benchmarks: List<BenchmarkDescriptor<Any?>>,
        start: () -> Unit,
        complete: () -> Unit
    )

    protected fun id(name: String, params: Map<String, String>): String {
        val id = if (params.isEmpty())
            name
        else
            name + params.entries.joinToString(prefix = " | ") { "${it.key}=${it.value}" }
        return id
    }
}

@KotlinxBenchmarkRuntimeInternalApi
fun runWithParameters(
    names: List<String>,
    parameters: Map<String, List<String>>,
    defaults: Map<String, List<String>>,
    function: (Map<String, String>) -> Unit
) {
    if (names.isEmpty()) {
        function(mapOf())
        return
    }

    fun parameterValues(name: String): List<String> {
        return parameters.getOrElse(name) {
            defaults.getOrElse(name) {
                error("No value specified for parameter '$name'")
            }
        }
    }

    val valueIndices = IntArray(names.size)
    val valueLimits = IntArray(names.size) {
        val name = names[it]
        parameterValues(name).size
    }
    while (true) {
        val paramsVariant = names.indices.associateBy({ names[it] }, {
            parameterValues(names[it])[valueIndices[it]]
        })
        function(paramsVariant)
        for (index in valueIndices.indices) {
            valueIndices[index]++
            if (valueIndices[index] < valueLimits[index])
                break
            else
                if (index == valueIndices.lastIndex)
                    return
            valueIndices[index] = 0
        }
    }
}