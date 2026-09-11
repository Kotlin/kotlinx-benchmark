@file:OptIn(KotlinxBenchmarkRuntimeInternalApi::class)

package kotlinx.benchmark.tests

import kotlinx.benchmark.BenchmarkConfiguration
import kotlinx.benchmark.BenchmarkDescriptorWithNoBlackholeParameter
import kotlinx.benchmark.BenchmarkReportFormatter
import kotlinx.benchmark.Blackhole
import kotlinx.benchmark.ReportBenchmarkResult
import kotlinx.benchmark.RunnerConfiguration
import kotlinx.benchmark.SuiteDescriptor
import kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi
import kotlin.test.Test
import kotlin.test.assertEquals

class BenchmarkReportFormatterTests {
    @Test
    fun csv() {
        val formatter = BenchmarkReportFormatter.create("csv")
        val suite = SuiteDescriptor(
            name = "example.CsvBenchmark",
            factory = { Unit },
            parametrize = { _, _ -> },
            setup = {},
            teardown = {},
            parameters = listOf("size", "label"),
            defaultParameters = emptyMap()
        )
        val runner = RunnerConfiguration(
            """
                name:test
                reportFile:report.csv
                traceFormat:text
                configurationName:main
                mode:avgt
                outputTimeUnit:ms
            """.trimIndent()
        )
        val config = BenchmarkConfiguration(runner, suite)
        val benchmark = BenchmarkDescriptorWithNoBlackholeParameter(
            name = "example.CsvBenchmark.work",
            suite = suite,
            blackhole = createTestBlackhole(),
            function = {}
        )
        val results = listOf(
            ReportBenchmarkResult(
                config = config,
                benchmark = benchmark,
                params = mapOf("size" to "10", "label" to "a,\"b\""),
                score = 1234.5,
                error = 0.25,
                confidence = 1234.25 to 1234.75,
                percentiles = emptyMap(),
                values = doubleArrayOf(1234.0, 1235.0)
            ),
            ReportBenchmarkResult(
                config = config,
                benchmark = benchmark,
                params = mapOf("size" to "20"),
                score = 2.0,
                error = 0.0,
                confidence = 2.0 to 2.0,
                percentiles = emptyMap(),
                values = doubleArrayOf(2.0, 2.0, 2.0)
            )
        )

        val expected = """
            "Benchmark","Mode","Threads","Samples","Score","Score Error (99.9%)","Unit",Param: size,Param: label
            "example.CsvBenchmark.work","avgt",1,2,1234.500000,0.250000,"ms/op","10","a,""b""${'"'}
            "example.CsvBenchmark.work","avgt",1,3,2.000000,0.000000,"ms/op","20",
        """.trimIndent().replace("\n", "\r\n") + "\r\n"

        assertEquals(expected, formatter.format(results, compilationMode = null, configurationName = "main"))
    }
}

internal expect fun createTestBlackhole(): Blackhole
