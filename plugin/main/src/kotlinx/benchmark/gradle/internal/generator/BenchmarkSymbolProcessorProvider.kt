package kotlinx.benchmark.gradle.internal.generator

import com.google.devtools.ksp.processing.*
import kotlinx.benchmark.gradle.Platform

internal class BenchmarkSymbolProcessorProvider(
    private val title: String,
    private val platform: Platform,
) : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        BenchmarkSymbolProcessor(title, platform, environment.codeGenerator)
}
