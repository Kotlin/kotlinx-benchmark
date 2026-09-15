package kotlinx.benchmark.integration

import kotlin.test.Test
import java.io.FileOutputStream

class KspIntegrationTest : GradleTest() {
    @Test
    fun analyzeLargeSourceFiles() {
        project("kotlin-native").let { runner ->
            val benchmarkSourceFile = file("src/commonMain/kotlin/CommonBenchmark.kt")

            // Pad file with a 30MB comment at the end.
            // After all, who wouldn't appreciate thoroughly commented code, right?
            FileOutputStream(benchmarkSourceFile, true).bufferedWriter().use { writer ->
                val chunk = "a".repeat(1024 * 1024) // 1MB

                writer.write("\n/*")
                repeat(30) {
                    writer.write(chunk)
                }
                writer.write("\n*/")
            }

            val target = "native"
            val capitalizedTarget = target.replaceFirstChar { it.uppercaseChar() }

            runner.runAndSucceed(":${target}BenchmarkGenerate")
            runner.runAndSucceed(":compile${capitalizedTarget}BenchmarkKotlin${capitalizedTarget}")
            runner.runAndSucceed(":${capitalizedTarget}Benchmark") {
                assertOutputContains("mathBenchmark")
            }
        }
    }
}
