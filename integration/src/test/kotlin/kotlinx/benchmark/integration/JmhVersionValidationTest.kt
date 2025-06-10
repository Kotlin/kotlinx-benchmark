package kotlinx.benchmark.integration

import kotlin.test.Test

class JmhVersionValidationTest : GradleTest() {
    @Test
    fun verifyWarningsAboutJmhVersions() {
        val runner = project("conflicting-jmh-versions", true) {}

        runner.runAndSucceed("assembleBenchmarks") {
            assertOutputContains("configures several JVM benchmarking targets that use different " +
                    "JMH versions (1.21 is used by jvmFirst; 1.22 is used by jvmSecond, jvmThird). " +
                    "Such configuration is not supported and may lead to runtime errors. " +
                    "Consider using the same JMH version across all benchmarking targets.")
            assertOutputContains("Configured JMH version (1.22) is older than a default version supplied by the benchmarking plugin")
            assertOutputContains("Configured JMH version (1.21) is older than a default version supplied by the benchmarking plugin")
        }
    }
}
