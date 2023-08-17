package kotlinx.benchmark.integration

import kotlin.test.*

class ConfigurationTest : GradleTest() {
    @Test
    fun testIterationsConfig() {
        val expectedIterations = 2

        val runner = project("kotlin-multiplatform") {
            configuration("iterationsConfig") {
                iterations = expectedIterations
            }
        }

        runner.run("nativeIterationsConfigBenchmark") {
            val actualIterations = this.output.lines().count { it.startsWith("Iteration #") }
            assertEquals(expectedIterations, actualIterations, "Expected $expectedIterations iterations but found $actualIterations")
        }
    }

    @Test
    fun testOutputTimeUnitConfig() {
        val expectedOutputTimeUnit = "ns"

        val runner = project("kotlin-multiplatform") {
            configuration("outputTimeUnitConfig") {
                outputTimeUnit = expectedOutputTimeUnit
            }
        }

        runner.run("nativeOutputTimeUnitConfigBenchmark") {
            val actualOutputTimeUnit = this.output.lines().find { it.contains("ops/$expectedOutputTimeUnit") }
            assertNotNull(actualOutputTimeUnit, "Expected output to specify time unit as $expectedOutputTimeUnit but was not found.")
        }
    }

    @Test
    fun testWarmUpConfig() {
        val expectedWarmUps = 2

        val runner = project("kotlin-multiplatform") {
            configuration("warmUpConfig") {
                warmups = expectedWarmUps
            }
        }

        runner.run("nativeWarmUpConfigBenchmark") {
            val actualWarmUps = this.output.lines().count { it.startsWith("Warm-up #") }
            assertEquals(expectedWarmUps, actualWarmUps, "Expected $expectedWarmUps warm-ups but found $actualWarmUps")
        }
    }

    @Test
    fun testAverageTimeModeConfig() {
        val expectedMode = "thrpt"

        val runner = project("kotlin-multiplatform") {
            configuration("averageTimeModeConfig") {
                mode = expectedMode
            }
        }

        runner.run("nativeAverageTimeModeConfigBenchmark") {
            val lines = this.output.lines()
            val summaryIndex = lines.indexOfFirst { it.contains("native summary:") }
            
            val benchmarkLine = lines[summaryIndex + 2]
            val actualMode = benchmarkLine.split(Regex("\\s+"))[1] // specified mode is in second column, two lines down from native summary

            assertEquals(expectedMode, actualMode, "Expected mode $expectedMode but found $actualMode")
        }
    }
}