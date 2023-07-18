package kotlinx.benchmark.integration

import java.io.*
import kotlin.test.*

class OptionsValidationTest : GradleTest() {

    @Test
    fun testIterationsValidation() {
        val runner = project("kotlin-multiplatform") {
            configuration("zeroIterations") {
                iterations = 0
                iterationTime = 100
                iterationTimeUnit = "ms"
            }
        }
    
        val exception = assertFailsWith<RuntimeException> {
            runner.run("zeroIterationsBenchmark")
        }
        assert(exception.message?.contains("Iterations must be greater than 0.") ?: false)
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
    
        val exception = assertFailsWith<RuntimeException> {
            runner.run("negativeWarmupsBenchmark")
        }
        assert(exception.message?.contains("Warmups must be equal to or greater than 0.") ?: false)
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
    
        val exception = assertFailsWith<RuntimeException> {
            runner.run("zeroIterationTimeBenchmark")
        }
        assert(exception.message?.contains("Iteration time must be greater than 0.") ?: false)
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
    
        val exception = assertFailsWith<RuntimeException> {
            runner.run("invalidIterationTimeUnitBenchmark")
        }
        assert(exception.message?.contains("Unknown time unit: x") ?: false)
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
    
        val exception = assertFailsWith<RuntimeException> {
            runner.run("invalidModeBenchmark")
        }
        assert(exception.message?.contains("Benchmark mode 'x' is not supported.") ?: false)
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
    
        val exception = assertFailsWith<RuntimeException> {
            runner.run("invalidOutputTimeUnitBenchmark")
        }
        assert(exception.message?.contains("Unknown time unit: x") ?: false)
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
    
        val exception = assertFailsWith<RuntimeException> {
            runner.run("blankIncludePatternBenchmark")
        }
        assert(exception.message?.contains("Include pattern should not be blank.") ?: false)
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
    
        val exception = assertFailsWith<RuntimeException> {
            runner.run("blankExcludePatternBenchmark")
        }
        assert(exception.message?.contains("Exclude pattern should not be blank.") ?: false)
    }      
}