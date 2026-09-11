package kotlinx.benchmark.integration

import kotlin.test.Test

class JarFileTest : GradleTest() {
    @Test
    fun testFilesFiltration() {
        val runner = project("jar-test", true) {}
        runner.runAndSucceed("jvmBenchmarkJar") {
            assertOutputDoesNotContain("Encountered duplicate path")
        }
    }
}
