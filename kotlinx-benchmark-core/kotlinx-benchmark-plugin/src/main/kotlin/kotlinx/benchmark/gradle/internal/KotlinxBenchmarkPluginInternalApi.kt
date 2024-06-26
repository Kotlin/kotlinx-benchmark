package kotlinx.benchmark.gradle.internal

import kotlin.RequiresOptIn.Level.WARNING

/**
 * Marks declarations that are **internal** to the Kotlinx Benchmark project.
 *
 * Classes or functions marked with this annotation have `public` visibility
 * for historical or technical reasons. They are not intended for use by
 * Kotlinx Benchmark users. They have no behaviour guarantees, and may lack clear
 * semantics, documentation, and backward compatibility.
 *
 * Please remove references to internal API to supported stable references.
 * If you still require access to these APIs, please raise an issue.
 */
@RequiresOptIn(
    message = "Internal Kotlinx Benchmark API. It may be changed in future releases without notice.",
    level = WARNING,
)
@MustBeDocumented
annotation class KotlinxBenchmarkPluginInternalApi
