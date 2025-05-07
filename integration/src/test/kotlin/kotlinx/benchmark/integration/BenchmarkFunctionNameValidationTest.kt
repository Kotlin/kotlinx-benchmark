package kotlinx.benchmark.integration

import kotlin.test.*

class BenchmarkFunctionNameValidationTest : GradleTest() {
    @Test
    fun jvmNamesValidations() {
        project("funny-names").apply {
            runAndFail("jvmBenchmarkCompile") {
                assertOutputContains("One or more benchmark functions are invalid and could not be processed by JMH. See logs for details.")
                assertOutputDoesNotContain("Group name should be the legal Java identifier")

                assertOutputContains("""Benchmark function name is a reserved Java keyword and cannot be used: "test.CommonBenchmark.assert" (declared as "test.CommonBenchmark.assert")""")

                assertOutputContains("""Benchmark function name is not a valid Java identifier: "test.BenchmarkBase.-illegal base name" (declared as "test.BenchmarkBase.base")""")
                val firstOccurrence = output.indexOf(""""test.BenchmarkBase.-illegal base name"""")
                assertEquals(-1, output.indexOf(""""test.BenchmarkBase.-illegal base name"""", firstOccurrence + 1),
                    "\"test.BenchmarkBase.-illegal base name\" is expected to be reported only once")

                assertOutputContains("""Benchmark function name is not a valid Java identifier: "test.CommonBenchmark.wrapString-gu_FwkY" (declared as "test.CommonBenchmark.wrapString")""")
                assertOutputContains("""Benchmark function name is not a valid Java identifier: "test.CommonBenchmark.-explicitlyRenamed" (declared as "test.CommonBenchmark.explicitlyRenamed")""")
                assertOutputContains("""Benchmark function name is not a valid Java identifier: "test.CommonBenchmark.backticked name" (declared as "test.CommonBenchmark.backticked name")""")
            }
        }
    }
}
