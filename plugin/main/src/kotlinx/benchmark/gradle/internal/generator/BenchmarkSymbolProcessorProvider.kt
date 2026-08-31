package kotlinx.benchmark.gradle.internal.generator

import com.google.devtools.ksp.processing.*

internal class BenchmarkSymbolProcessorProvider(
    private val title: String,
    private val platform: Platform,
) : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        BenchmarkSymbolProcessor(title, platform, environment.codeGenerator)
}
