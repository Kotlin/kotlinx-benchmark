package kotlinx.benchmark.integration

import java.io.*
import kotlin.test.*

class ReportFormatTest : GradleTest() {

    @Test
    fun testReportFormatFileNames() {
        val formats = listOf(null, "json", "csv", "scsv", "text")

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

        formats.forEach {
            val name = it ?: "jsonDefault"
            val ext = it ?: "json"
            runner.run("${name}Benchmark")
            val reports = reports(name)
            assertEquals(3, reports.size)
            assertEquals(setOf("js.$ext", "jvm.$ext", "native.$ext"), reports.map(File::getName).toSet())
        }
    }
}
