package kotlinx.benchmark.integration

import kotlin.test.*
import org.gradle.testkit.runner.BuildResult

class ConfigurationTest : GradleTest() {

    private fun testConfiguration(
        setupBlock: BenchmarkConfiguration.() -> Unit,
        checkBlock: BuildResult.() -> Unit
    ) {
        project("kotlin-multiplatform") {
            configuration("config") {
                setupBlock()
            }
        }.run("nativeConfigBenchmark") {
            checkBlock()
        }
    }

    @Test
    fun testIterationsConfig() {
        val expectedIterations = 2

        testConfiguration(
            setupBlock = {
                iterations = expectedIterations
            },
            checkBlock = {
                val actualIterations = output.lines().count { it.startsWith("Iteration #") }
                assertEquals(expectedIterations, actualIterations)
            }
        )
    }

    @Test
    fun testOutputTimeUnitConfig() {
        val expectedOutputTimeUnit = "ns"

        testConfiguration(
            setupBlock = {
                outputTimeUnit = expectedOutputTimeUnit
            },
            checkBlock = {
                val actualOutputTimeUnit = output.lines().find { it.contains("ops/$expectedOutputTimeUnit") }
                assertNotNull(actualOutputTimeUnit, "Expected output to specify time unit as $expectedOutputTimeUnit but was not found.")
            }
        )
    }

    @Test
    fun testWarmUpConfig() {
        val expectedWarmUps = 2

        testConfiguration(
            setupBlock = {
                warmups = expectedWarmUps
            },
            checkBlock = {
                val actualWarmUps = output.lines().count { it.startsWith("Warm-up #") }
                assertEquals(expectedWarmUps, actualWarmUps, "Expected $expectedWarmUps warm-ups but found $actualWarmUps")
            }
        )
    }

    @Test
    fun testAverageTimeModeConfig() {
        val expectedMode = "thrpt"

        testConfiguration(
            setupBlock = {
                mode = expectedMode
            },
            checkBlock = {
                val lines = output.lines()
                val summaryIndex = lines.indexOfFirst { it.contains("native summary:") }
                val benchmarkLine = lines[summaryIndex + 2]
                val actualMode = benchmarkLine.split(Regex("\\s+"))[1]
                assertEquals(expectedMode, actualMode, "Expected mode $expectedMode but found $actualMode")
            }
        )
    }
}
