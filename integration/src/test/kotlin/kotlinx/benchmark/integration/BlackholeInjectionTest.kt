package kotlinx.benchmark.integration

import kotlin.test.Test

class BlackholeInjectionTest : GradleTest()  {
    private fun runForTarget(target: String) {
        project("kotlin-multiplatform-with-blackhole")
            .runAndSucceed("${target}Benchmark")
    }
    @Test
    fun native() = runForTarget("native")

    @Test
    fun js() = runForTarget("js")

    @Test
    fun wasmJs() = runForTarget("wasmJs")

    @Test
    fun jvm() = runForTarget("jvm")
}
