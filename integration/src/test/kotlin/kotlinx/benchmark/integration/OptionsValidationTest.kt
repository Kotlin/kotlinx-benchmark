package kotlinx.benchmark.integration

import kotlin.test.*

class OptionsValidationTest : GradleTest() {

    @Test
    fun testReportFormatValidation() {
        val runner = project("kotlin-multiplatform") {
            configuration("invalidReportFormat") {
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                reportFormat = "htmll"
            }
        }

        runner.runAndFail("invalidReportFormatBenchmark") {
            assertOutputContains("Invalid report format: 'htmll'. Accepted formats: ${ValidOptions.format.joinToString(", ")} (e.g., reportFormat = \"json\").")
        }
    }

    @Test
    fun testIterationsValidation() {
        val runner = project("kotlin-multiplatform") {
            configuration("zeroIterations") {
                iterations = 0
                iterationTime = 100
                iterationTimeUnit = "ms"
            }
        }

        runner.runAndFail("zeroIterationsBenchmark") {
            assertOutputContains("Invalid iterations: '0'. Expected a positive integer (e.g., iterations = 5).")
        }
    }

    @Test
    fun testWarmupsValidation() {
        val runner = project("kotlin-multiplatform") {
            configuration("negativeWarmups") {
                iterations = 1
                warmups = -1
                iterationTime = 100
                iterationTimeUnit = "ms"
            }
        }

        runner.runAndFail("negativeWarmupsBenchmark") {
            assertOutputContains("Invalid warmups: '-1'. Expected a non-negative integer (e.g., warmups = 3).")
        }
    }

    @Test
    fun testIterationTimeValidation() {
        val runner = project("kotlin-multiplatform") {
            configuration("zeroIterationTime") {
                iterations = 1
                iterationTime = 0
                iterationTimeUnit = "ms"
            }
            configuration("missingIterationTimeUnit") {
                iterations = 1
                iterationTime = 1
            }
        }

        runner.runAndFail("zeroIterationTimeBenchmark") {
            assertOutputContains("Invalid iterationTime: '0'. Expected a positive number (e.g., iterationTime = 300).")
        }
        runner.runAndFail("missingIterationTimeUnitBenchmark") {
            assertOutputContains("Missing iterationTimeUnit. Please provide iterationTimeUnit when specifying iterationTime.")
        }
    }

    @Test
    fun testIterationTimeUnitValidation() {
        val runner = project("kotlin-multiplatform") {
            configuration("invalidIterationTimeUnit") {
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "x"
            }
            configuration("incorrectCaseIterationTimeUnit") {
                iterations = 1
                iterationTime = 1
                iterationTimeUnit = "seconds"
            }
            configuration("missingIterationTime") {
                iterations = 1
                iterationTimeUnit = "s"
            }
        }

        runner.runAndFail("invalidIterationTimeUnitBenchmark") {
            assertOutputContains("Invalid iterationTimeUnit: 'x'. Accepted units: ${ValidOptions.timeUnits.joinToString(", ")} (e.g., iterationTimeUnit = \"ms\").")
        }
        runner.runAndFail("incorrectCaseIterationTimeUnitBenchmark") {
            assertOutputContains("Invalid iterationTimeUnit: 'seconds'. Accepted units: ${ValidOptions.timeUnits.joinToString(", ")} (e.g., iterationTimeUnit = \"ms\").")
        }
        runner.runAndFail("missingIterationTimeBenchmark") {
            assertOutputContains("Missing iterationTime. Please provide iterationTime when specifying iterationTimeUnit.")
        }
    }

    @Test
    fun testValidOptions() {
        val runner = project("kotlin-multiplatform") {
            configuration("validOptions") {
                iterations = 1
                iterationTime = 1
                iterationTimeUnit = "SECONDS"
                mode = "AverageTime"
            }
        }
        runner.run("validOptionsBenchmark") // Successful
    }

    @Test
    fun testModeValidation() {
        val runner = project("kotlin-multiplatform") {
            configuration("invalidMode") {
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                mode = "x"
            }
        }

        runner.runAndFail("invalidModeBenchmark") {
            assertOutputContains("Invalid benchmark mode: 'x'. Accepted modes: ${ValidOptions.modes.joinToString(", ")} (e.g., mode = \"thrpt\").")
        }
    }

    @Test
    fun testOutputTimeUnitValidation() {
        val runner = project("kotlin-multiplatform") {
            configuration("invalidOutputTimeUnit") {
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                outputTimeUnit = "x"
            }
        }

        runner.runAndFail("invalidOutputTimeUnitBenchmark") {
            assertOutputContains("Invalid outputTimeUnit: 'x'. Accepted units: ${ValidOptions.timeUnits.joinToString(", ")} (e.g., outputTimeUnit = \"ns\").")
        }
    }

    @Test
    fun testIncludesValidation() {
        val runner = project("kotlin-multiplatform") {
            configuration("blankIncludePattern") {
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                include(" ")
            }
        }

        runner.runAndFail("blankIncludePatternBenchmark") {
            assertOutputContains("Invalid include pattern: ' '. Pattern must not be blank.")
        }
    }

    @Test
    fun testExcludesValidation() {
        val runner = project("kotlin-multiplatform") {
            configuration("blankExcludePattern") {
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                exclude(" ")
            }
        }

        runner.runAndFail("blankExcludePatternBenchmark") {
            assertOutputContains("Invalid exclude pattern: ' '. Pattern must not be blank.")
        }
    }  

    @Test
    fun testParamsValidation() {
        val runner = project("kotlin-multiplatform") {
            configuration("blankParamName") {
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                param(" ", 5)
            }
        }

        runner.runAndFail("blankParamNameBenchmark") {
            assertOutputContains("Invalid param name: ' '. It must not be blank.")
        }
    }

    @Test
    fun testAdvancedValidation() {
        val runner = project("kotlin-multiplatform") {
            configuration("blankAdvancedConfigName") {
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                advanced(" ", "value")
            }

            configuration("invalidAdvancedConfigName") {
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                advanced("jsFork", "value")
            }

            configuration("invalidNativeFork") {
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                advanced("nativeFork", "x")
            }

            configuration("invalidNativeGCAfterIteration") {
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                advanced("nativeGCAfterIteration", "x")
            }

            configuration("invalidJvmForks") {
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                advanced("jvmForks", "-1")
            }

            configuration("invalidJsUseBridge") {
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                advanced("jsUseBridge", "x")
            }
        }

        runner.runAndFail("blankAdvancedConfigNameBenchmark") {
            assertOutputContains("Invalid advanced option name: ' '. It must not be blank.")
        }
        runner.runAndFail("invalidAdvancedConfigNameBenchmark") {
            assertOutputContains("Invalid advanced option name: 'jsFork'. Accepted options: \"nativeFork\", \"nativeGCAfterIteration\", \"jvmForks\", \"jsUseBridge\".")
        }
        runner.runAndFail("invalidNativeForkBenchmark") {
            assertOutputContains("Invalid value for 'nativeFork': 'x'. Accepted values: ${ValidOptions.nativeForks.joinToString(", ")}.")
        }
        runner.runAndFail("invalidNativeGCAfterIterationBenchmark") {
            assertOutputContains("Invalid value for 'nativeGCAfterIteration': 'x'. Expected a Boolean value.")
        }
        runner.runAndFail("invalidJvmForksBenchmark") {
            assertOutputContains("Invalid value for 'jvmForks': '-1'. Expected a non-negative integer or \"definedByJmh\".")
        }
        runner.runAndFail("invalidJsUseBridgeBenchmark") {
            assertOutputContains("Invalid value for 'jsUseBridge': 'x'. Expected a Boolean value.")
        }
    }
}

private object ValidOptions {
    val format = setOf("json", "csv", "scsv", "text")
    val timeUnits = setOf(
        "NANOSECONDS", "ns", "nanos",
        "MICROSECONDS", "us", "micros",
        "MILLISECONDS", "ms", "millis",
        "SECONDS", "s", "sec",
        "MINUTES", "m", "min"
    )
    val modes = setOf("thrpt", "avgt", "Throughput", "AverageTime")
    val nativeForks = setOf("perBenchmark", "perIteration")
}