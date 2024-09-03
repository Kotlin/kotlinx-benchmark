package kotlinx.benchmark.gradle.internal.generator

import kotlin.RequiresOptIn.Level.ERROR

/**
 * This annotation indicates that a dependency on `kotlin-compiler-embeddable` is required at runtime.
 *
 * If code has this annotation, it **must** only be used inside a Gradle Worker with an isolated classpath.
 * The worker must have `kotlin-compiler-embeddable` available on the classpath.
 *
 * @see kotlinx.benchmark.gradle.internal.generator.workers.GenerateJsSourceWorker
 * @see kotlinx.benchmark.gradle.internal.generator.workers.GenerateWasmSourceWorker
 * @see kotlinx.benchmark.gradle.NativeSourceGeneratorWorker
 */
@RequiresOptIn(level = ERROR)
@MustBeDocumented
internal annotation class RequiresKotlinCompilerEmbeddable
