package kotlinx.benchmark.integration

import java.io.*
import kotlin.test.*

class SourceSetAsBenchmarkTargetTest : GradleTest() {

    @Test
    fun testSupportForSourceSetsAsBenchmarkTargets() {
        val jvmBenchmark = "jvmBenchmark"
        val configuration = "jsonDefault"
        val targets = listOf("jsIr", "wasmJs", "jvm", "native", jvmBenchmark)

        val runner =
            project("kotlin-multiplatform", true) {
                configuration(configuration) {
                    iterations = 1
                    iterationTime = 100
                    iterationTimeUnit = "ms"
                }
                register(jvmBenchmark) { jmhVersion = "1.21" }
            }

        runner.run("${configuration}Benchmark")
        val reports = reports(configuration)
        assertEquals(targets.size, reports.size)
        assertEquals(targets.map { "$it.json" }.toSet(), reports.map(File::getName).toSet())
    }
}
