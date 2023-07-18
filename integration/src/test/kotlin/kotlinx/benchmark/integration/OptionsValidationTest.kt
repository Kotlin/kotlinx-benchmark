package kotlinx.benchmark.integration

import java.io.*
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
            assertOutputContains("Report format 'htmll' is not supported.")
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
            assertOutputContains("Iterations must be greater than 0.")
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
            assertOutputContains("Warmups must be equal to or greater than 0.")
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
        }
    
        runner.runAndFail("zeroIterationTimeBenchmark") {
            assertOutputContains("Iteration time must be greater than 0.")
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
        }
    
        runner.runAndFail("invalidIterationTimeUnitBenchmark") {
            assertOutputContains("Unknown time unit: x")
        }
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
            assertOutputContains("Benchmark mode 'x' is not supported.")
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
            assertOutputContains("Unknown time unit: x")
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
            assertOutputContains("Include pattern should not be blank.")
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
            assertOutputContains("Exclude pattern should not be blank.")
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
            assertOutputContains("Param name should not be blank.")
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
        }

        assertFailsWith<RuntimeException> {
            runner.run("blankAdvancedConfigNameBenchmark")
        }
    }
}