package kotlinx.benchmark.gradle

import kotlin.RequiresOptIn.Level.WARNING

/**
 * Marks declarations that belong to an **experimental** API of kotlinx-benchmark.
 *
 * This API is not stable yet and may change, evolve, or be removed in future
 * releases without notice. Use it only if you are prepared to update your code
 * accordingly.
 */
@RequiresOptIn(
    message = "Experimental kotlinx-benchmark API. It may be changed in future releases without notice.",
    level = WARNING,
)
@MustBeDocumented
annotation class KotlinxBenchmarkPluginExperimentalApi