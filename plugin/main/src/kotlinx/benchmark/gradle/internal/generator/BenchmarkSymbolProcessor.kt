package kotlinx.benchmark.gradle.internal.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import kotlinx.benchmark.gradle.Platform
import kotlinx.benchmark.gradle.SuiteSourceGeneratorWithKSP


internal class BenchmarkSymbolProcessor(
    val title: String,
    val platform: Platform,
    val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    private val sourceGenerator = SuiteSourceGeneratorWithKSP(title, codeGenerator, platform)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        sourceGenerator.generate(resolver)
        return emptyList()
    }

    override fun finish() {
        sourceGenerator.generateRunnerMain()
    }
}