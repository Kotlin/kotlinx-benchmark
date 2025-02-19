package kotlinx.benchmark.integration

import kotlin.test.Test

class KotlinJsTest : GradleTest() {
    @Test
    fun useEsModules() {
        project("es-modules", true).runAndSucceed("jsEsBenchmark") {
            assertOutputContains("CommonBenchmark.benchmark")
        }
    }

    @Test
    fun useUmdModules() {
        project("es-modules", true).runAndSucceed("jsUmdBenchmark") {
            assertOutputContains("CommonBenchmark.benchmark")
        }
    }

    @Test
    fun useCommonJs() {
        project("es-modules", true).runAndSucceed("jsCommonBenchmark") {
            assertOutputContains("CommonBenchmark.benchmark")
        }
    }
}
