package kotlinx.benchmark.gradle.internal

import org.gradle.api.provider.Provider
import java.io.File

/**
 * Resolves the report location of the current build.
 *
 * The location must be resolved from a task action. A [Provider] derived from
 * [BenchmarkReportTimeService] is evaluated and frozen when the configuration cache entry is
 * stored, while the service itself is recreated on every build.
 */
@KotlinxBenchmarkPluginInternalApi
class BenchmarkReportLocation @KotlinxBenchmarkPluginInternalApi constructor(
    private val reportTime: Provider<BenchmarkReportTimeService>,
    private val buildDir: File,
    private val reportsDir: String,
    private val fileName: String,
) {
    val directory: File
        get() = buildDir.resolve("$reportsDir/${reportTime.get().timestamp}")

    val file: File
        get() = directory.resolve(fileName)
}
