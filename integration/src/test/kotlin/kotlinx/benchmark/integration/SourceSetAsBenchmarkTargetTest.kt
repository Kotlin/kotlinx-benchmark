package kotlinx.benchmark.integration

import java.io.*
import kotlin.test.*

class SourceSetAsBenchmarkTargetTest : GradleTest() {

    @Test
    fun testSupportForSourceSetsAsBenchmarkTargets() {
        val targets = listOf("jvmCustom", "jsCustom")

        val runner =
            project("kotlin-multiplatform-separate-source-set", true) {
                configuration("csv") {
                    iterations = 1
                    iterationTime = 100
                    iterationTimeUnit = "ms"
                    reportFormat = "csv"
                    advanced("jmhIgnoreLock", true)
                }
            }

        runner.runAndSucceed("benchmark") {
            assertTasksExecuted(targets.map { ":${it}Benchmark" })
        }
        val jsonReports = reports("main").map(File::getName).filter { it.endsWith(".json") }
        assertEquals(targets.map { "$it.json" }.toSet(), jsonReports.toSet())

        runner.runAndSucceed("csvBenchmark") {
            assertTasksExecuted(targets.map { ":${it}CsvBenchmark" })
        }
        val csvReports = reports("csv").map(File::getName).filter { it.endsWith(".csv") }
        assertEquals(targets.map { "$it.csv" }.toSet(), csvReports.toSet())
    }
}
