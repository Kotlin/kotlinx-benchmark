package kotlinx.benchmark.gradle.internal.generator

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated

internal class BenchmarkSymbolProcessor(
    title: String,
    platform: Platform,
    codeGenerator: CodeGenerator,
) : SymbolProcessor {
    private val sourceGenerator = SuiteSourceGenerator(title, codeGenerator, platform)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        sourceGenerator.generate(resolver)
        return emptyList()
    }

    override fun finish() {
        sourceGenerator.generateRunnerMain()
    }
}
