package kotlinx.benchmark.integration

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportFormatTest : GradleTest() {

    @Test
    fun testReportFormatFileNames() {
        val formats = listOf(null, "json", "csv", "scsv", "text")
        val targets = listOf("js", "wasmJs", "wasmWasi", "jvm", "native")

        val runner = project("kotlin-multiplatform", true) {
            formats.forEach { format ->
                configuration(format ?: "jsonDefault") {
                    warmups = 1
                    iterations = 1
                    iterationTime = 100
                    iterationTimeUnit = "ms"
                    reportFormat = format
                    advanced("jmhIgnoreLock", true)
                }
            }
        }

        formats.forEach { format ->
            val name = format ?: "jsonDefault"
            val ext = format ?: "json"
            runner.runAndSucceed("${name}Benchmark")
            val reports = reports(name)
            assertEquals(targets.size, reports.size)
            assertEquals(targets.map { "$it.$ext" }.toSet(), reports.map(File::getName).toSet())
        }
    }

    @Test
    fun testConfigurationName() {
        val targets = listOf("native", "js", "wasmJs", "wasmWasi")
        checkJsonReport(targets) { target, report ->
            for (benchmark in report.jsonArray) {
                benchmark as JsonObject
                assertTrue(benchmark.containsKey("configurationName"))
                assertEquals(
                    "testConfig", benchmark["configurationName"]!!.jsonPrimitive.content,
                    "Incorrect configuration name in a report for target $target"
                )
            }
        }
    }

    @Test
    fun testCompilationMode() {
        val webTargets = listOf("js", "wasmJs", "wasmWasi")
        val targets = webTargets + "native"
        checkJsonReport(targets) { target, report ->
            for (benchmark in report.jsonArray) {
                benchmark as JsonObject
                assertTrue(benchmark.containsKey("compilationMode"))
                val mode = benchmark["compilationMode"]!!.jsonPrimitive.content
                if (target in webTargets) {
                    assertEquals("Production", mode)
                } else {
                    assertEquals("Debug", mode)
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun checkJsonReport(targets: List<String>, block: (String, JsonElement) -> Unit) {
        val configName = "testConfig"
        val runner = project("kotlin-multiplatform", true) {
            configuration(configName) {
                warmups = 1
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                reportFormat = "json"
            }
        }
        runner.runAndSucceed(*targets.map { target -> "${target}TestConfigBenchmark" }.toTypedArray())

        for (target in targets) {
            val report = reports(configName).single {
                it.extension == "json" && it.nameWithoutExtension == target
            }

            val element = report.inputStream().use { inputStream ->
                Json.decodeFromStream<JsonElement>(inputStream)
            }
            block(target, element)
        }
    }
}
