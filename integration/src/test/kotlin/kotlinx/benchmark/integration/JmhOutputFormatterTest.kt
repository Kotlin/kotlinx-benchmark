package kotlinx.benchmark.integration

import kotlin.test.Test

class JmhOutputFormatterTest : GradleTest() {
    @Test
    fun verifyOutputFormatting() {
        val runner = project("verbose-benchmark") {}

        runner.runAndSucceed("benchmark") {
            assertOutputContains("Hello, stderr!")
        }
    }
}
