package kotlinx.benchmark.integration

import kotlin.test.Test

class KotlinJsTest : GradleTest() {
    @Test
    fun useEsModules() {
        project("es-modules").runAndSucceed("jsEsBenchmark") {
            assertOutputContains("CommonBenchmark.benchmark")
        }
    }

    @Test
    fun useUmdModules() {
        project("es-modules").runAndSucceed("jsUmdBenchmark") {
            assertOutputContains("CommonBenchmark.benchmark")
        }
    }

    @Test
    fun useCommonJs() {
        project("es-modules").runAndSucceed("jsCommonBenchmark") {
            assertOutputContains("CommonBenchmark.benchmark")
        }
    }
}
