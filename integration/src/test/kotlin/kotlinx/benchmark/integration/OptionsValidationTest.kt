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
            assertOutputContains("Invalid report format: 'htmll'. Accepted formats: json, csv, scsv, text (e.g., reportFormat = 'json').")
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
        }
    
        runner.runAndFail("zeroIterationTimeBenchmark") {
            assertOutputContains("Invalid iterationTime: '0'. Expected a positive number (e.g., iterationTime = 300).")
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
            assertOutputContains("Invalid iterationTimeUnit: 'x'. Accepted units: seconds, s, microseconds, us, milliseconds, ms, nanoseconds, ns, minutes, m (e.g., iterationTimeUnit = 'ms').")
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
            assertOutputContains("Invalid benchmark mode: 'x'. Accepted modes: thrpt, avgt (e.g., mode = 'thrpt').")
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
            assertOutputContains("Invalid outputTimeUnit: 'x'. Accepted units: seconds, s, microseconds, us, milliseconds, ms, nanoseconds, ns, minutes, m (e.g., outputTimeUnit = 'ns').")
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
            assertOutputContains("Invalid advanced config name: ' '. It must not be blank.")
        }
        runner.runAndFail("invalidNativeForkBenchmark") {
            assertOutputContains("Invalid value 'x' for 'nativeFork'. Accepted values: 'perBenchmark', 'perIteration' (e.g., nativeFork = 'perBenchmark').")
        }
        runner.runAndFail("invalidNativeGCAfterIterationBenchmark") {
            assertOutputContains("Invalid value 'x' for 'nativeGCAfterIteration'. Expected a boolean value (e.g., nativeGCAfterIteration = true).")
        }
        runner.runAndFail("invalidJvmForksBenchmark") {
            assertOutputContains("Invalid value '-1' for 'jvmForks'. Expected a non-negative integer or 'definedByJmh' (e.g., jvmForks = 2 or jvmForks = 'definedByJmh').")
        }
        runner.runAndFail("invalidJsUseBridgeBenchmark") {
            assertOutputContains("Invalid value 'x' for 'jsUseBridge'. Expected a boolean value (e.g., jsUseBridge = true).")
        }
    }      
}