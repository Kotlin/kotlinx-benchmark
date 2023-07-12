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
        
        assertFailsWith<RuntimeException> {
            runner.run("invalidReportFormatBenchmark")
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
        
        assertFailsWith<RuntimeException> {
            runner.run("zeroIterationsBenchmark")
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
        
        assertFailsWith<RuntimeException> {
            runner.run("negativeWarmupsBenchmark")
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
        
        assertFailsWith<RuntimeException> {
            runner.run("zeroIterationTimeBenchmark")
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
        
        assertFailsWith<RuntimeException> {
            runner.run("invalidIterationTimeUnitBenchmark")
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
        
        assertFailsWith<RuntimeException> {
            runner.run("invalidModeBenchmark")
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
        
        assertFailsWith<RuntimeException> {
            runner.run("invalidOutputTimeUnitBenchmark")
        }
    }
}
