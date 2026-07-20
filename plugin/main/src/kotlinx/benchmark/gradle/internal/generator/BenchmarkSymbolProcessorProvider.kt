package kotlinx.benchmark.gradle.internal.generator

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import kotlinx.benchmark.gradle.Platform

internal class BenchmarkSymbolProcessorProvider(val title: String, val platform: Platform) : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return BenchmarkSymbolProcessor(title, platform, environment.codeGenerator)
    }
}