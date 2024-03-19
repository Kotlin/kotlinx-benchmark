package kotlinx.benchmark.integration

import kotlin.test.*

class JvmProfilerTest : GradleTest() {

    @Test
    fun testGcProfiler() {
        val runner = project("kotlin-multiplatform") {
            configuration("gcProfiler") {
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                advanced("jvmProfiler", "gc")
            }
        }

        runner.run("jvmGcProfilerBenchmark") {
            assertOutputContains("gc.alloc.rate")
            assertOutputContains("BUILD SUCCESSFUL")
        }
    }

    @Test
    fun testStackProfilerEffect() {
        val runner = project("kotlin-multiplatform") {
            configuration("stackProfiler") {
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                advanced("jvmProfiler", "stack")
            }
        }

        runner.run("jvmStackProfilerBenchmark") {
            assertOutputContains("stack")
            assertOutputContains("BUILD SUCCESSFUL")
        }
    }

    @Test
    fun testClProfiler() {
        val runner = project("kotlin-multiplatform") {
            configuration("clProfiler") {
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                advanced("jvmProfiler", "cl")
            }
        }

        runner.run("jvmClProfilerBenchmark") {
            assertOutputContains("class.unload.norm")
            assertOutputContains("BUILD SUCCESSFUL")
        }
    }

    @Test
    fun testCompProfilerEffect() {
        val runner = project("kotlin-multiplatform") {
            configuration("compProfiler") {
                iterations = 1
                iterationTime = 100
                iterationTimeUnit = "ms"
                advanced("jvmProfiler", "comp")
            }
        }

        runner.run("jvmCompProfilerBenchmark") {
            assertOutputContains("compiler.time.profiled")
            assertOutputContains("BUILD SUCCESSFUL")
        }
    }
}
