package kotlinx.benchmark.gradle.internal

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Timestamp that names the report directory of the current build.
 *
 * A build service is instantiated once per build and is never stored in the configuration cache,
 * so every build gets its own report directory.
 */
@KotlinxBenchmarkPluginInternalApi
abstract class BenchmarkReportTimeService : BuildService<BuildServiceParameters.None> {
    val timestamp: String = LocalDateTime.now()
        .format(DateTimeFormatter.ISO_DATE_TIME)
        .replace(":", ".") // Windows doesn't allow ':' in path
}
