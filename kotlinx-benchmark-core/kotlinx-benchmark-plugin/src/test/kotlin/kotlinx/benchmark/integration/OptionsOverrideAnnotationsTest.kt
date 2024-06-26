package kotlinx.benchmark.integration

import kotlin.test.*

class OptionsOverrideAnnotationsTest : GradleTest() {

    private fun testConfiguration(
        setupBlock: BenchmarkConfiguration.() -> Unit,
        checkBlock: (reportText: String, consoleOutput: String) -> Unit
    ) {
        project("config-options") {
            configuration("config") {
                setupBlock()
            }
        }.run("nativeConfigBenchmark") {
            val reportText = reports("config").single().readText()
            checkBlock(reportText, output)
        }
    }

    private fun Regex.testMatches(reportText: String, expectedResult: String) =
        testMatches(reportText, listOf(expectedResult))

    private fun Regex.testMatches(reportText: String, expectedResults: List<String>) {
        var match = this.find(reportText)
        for (expectedResult in expectedResults) {
            assertNotNull(match, message = "Expected $expectedResult, but was null")
            val result = match.groupValues[1]
            assertEquals(expectedResult, result)

            match = match.next()
        }
        assertNull(match)
    }

    @Test
    fun testIterations() {
        val expectedIterations = 2

        testConfiguration(
            setupBlock = {
                iterations = expectedIterations
            },
            checkBlock = { reportText, consoleOutput ->
                Regex(""""measurementIterations" : (\d+)""")
                    .testMatches(reportText, "$expectedIterations")

                Regex("^Iteration #(\\d+)", RegexOption.MULTILINE)
                    .testMatches(consoleOutput, List(expectedIterations) { "$it" })

            }
        )
    }

    @Test
    fun testWarmups() {
        val expectedWarmups = 2

        testConfiguration(
            setupBlock = {
                warmups = expectedWarmups
            },
            checkBlock = { reportText, consoleOutput ->
                Regex(""""warmupIterations" : (\d+)""")
                    .testMatches(reportText, "$expectedWarmups")

                Regex("^Warm-up #(\\d+)", RegexOption.MULTILINE)
                    .testMatches(consoleOutput, List(expectedWarmups) { "$it" })
            }
        )
    }

    @Test
    fun testIterationTime() {
        val expectedIterationTime = "400 ms"

        testConfiguration(
            setupBlock = {
                iterationTime = 400
                iterationTimeUnit = "ms"
            },
            checkBlock = { reportText, _ ->
                Regex(""""measurementTime" : "(.*)"""")
                    .testMatches(reportText, expectedIterationTime)
                Regex(""""warmupTime" : "(.*)"""")
                    .testMatches(reportText, expectedIterationTime)
            }
        )
    }

    @Test
    fun testOutputTimeUnit() {
        val expectedOutputTimeUnit = "ns"
        val expectedCount = /*warmups*/3 + /*iterations*/5 + /*Success:*/1 + /*summary*/1

        testConfiguration(
            setupBlock = {
                outputTimeUnit = expectedOutputTimeUnit
            },
            checkBlock = { reportText, consoleOutput ->
                Regex(""""scoreUnit" : "(.*)"""")
                    .testMatches(reportText, "ops/$expectedOutputTimeUnit")

                Regex("ops/(\\w+)")
                    .testMatches(consoleOutput, List(expectedCount) { expectedOutputTimeUnit })
            }
        )
    }

    @Test
    fun testMode() {
        val expectedMode = "avgt"
        val expectedCount = /*warmups*/3 + /*iterations*/5 + /*Success:*/1 + /*summary*/1

        testConfiguration(
            setupBlock = {
                mode = expectedMode
            },
            checkBlock = { reportText, consoleOutput ->
                Regex(""""mode" : "(.*)"""")
                    .testMatches(reportText, expectedMode)

                Regex("(\\w+)/op")
                    .testMatches(consoleOutput, List(expectedCount) { "ms" })

                val lines = consoleOutput.lines()
                val summaryIndex = lines.indexOfFirst { it.contains("native summary:") }
                val benchmarkLine = lines[summaryIndex + 2]
                val mode = benchmarkLine.split(Regex("\\s+"))[2]
                assertEquals(expectedMode, mode, benchmarkLine)
            }
        )
    }

    @Test
    fun testParams() {
        testConfiguration(
            setupBlock = {
                param("data", 5.0, 12.5)
            },
            checkBlock = { reportText, consoleOutput ->
                Regex(""""params" : \{\s*"data" : "(\S*)"\s*}""")
                    .testMatches(reportText, listOf("5.0", "12.5"))

                Regex("test.CommonBenchmark.mathBenchmark [|] data=(.*)")
                    .testMatches(consoleOutput, listOf("5.0", "12.5"))
            }
        )
    }
}
