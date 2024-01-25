package kotlinx.benchmark.integration

import org.junit.Test

class JvmToolchainsTest : GradleTest() {
    @Test
    fun testJvmToolchainSetup() {
        val runner = project("kmp-with-toolchain", true, GradleTestVersion.v8_0) {
        }
        runner.run("jvmBenchmark")
    }
}
