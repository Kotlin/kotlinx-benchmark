package kotlinx.benchmark.integration

import java.io.*
import kotlin.test.*

class ReportFormatTest : GradleTest() {

    @Test
    fun testReportFormatFileNames() {
        val formats = listOf(null, "json", "csv", "scsv", "text")
        val targets = listOf("jsIr", "wasmJs", "jvm", "native")

        val runner = project("kotlin-multiplatform", true) {
            formats.forEach { format ->
                configuration(format ?: "jsonDefault") {
                    iterations = 1
                    iterationTime = 100
                    iterationTimeUnit = "ms"
                    reportFormat = format
                }
            }
        }

        formats.forEach { format ->
            val name = format ?: "jsonDefault"
            val ext = format ?: "json"
            runner.run("${name}Benchmark")
            val reports = reports(name)
            assertEquals(targets.size, reports.size)
            assertEquals(targets.map { "$it.$ext" }.toSet(), reports.map(File::getName).toSet())
        }
    }
}
