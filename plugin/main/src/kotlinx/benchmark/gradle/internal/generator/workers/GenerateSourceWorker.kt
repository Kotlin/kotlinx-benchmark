package kotlinx.benchmark.gradle.internal.generator.workers

import kotlinx.benchmark.gradle.internal.generator.BenchmarkSourceGenerator
import kotlinx.benchmark.gradle.internal.generator.Platform
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

internal abstract class GenerateSourceWorker : WorkAction<GenerateSourceWorker.Params> {
    internal interface Params : WorkParameters {
        val title: Property<String>
        val platform: Property<Platform>
        val sourceRoots: ConfigurableFileCollection
        val inputDependencies: ConfigurableFileCollection
        val outputSourcesDir: DirectoryProperty
        val outputResourcesDir: DirectoryProperty
        val outputClassesDir: DirectoryProperty
        val projectDir: DirectoryProperty
        val outputBaseDir: DirectoryProperty
        val cachesDir: DirectoryProperty
        val languageVersion: Property<String>
        val apiVersion: Property<String>
        val nativeTarget: Property<String>
    }

    override fun execute() {
        BenchmarkSourceGenerator.generate(
            title = parameters.title.get(),
            platform = parameters.platform.get(),
            sourceRoots = parameters.sourceRoots.files.toList(),
            inputDependencies = parameters.inputDependencies.files.toList(),
            outputSourcesDir = parameters.outputSourcesDir.get().asFile,
            outputResourcesDir = parameters.outputResourcesDir.get().asFile,
            outputClassesDir = parameters.outputClassesDir.get().asFile,
            projectDir = parameters.projectDir.get().asFile,
            outputBaseDir = parameters.outputBaseDir.get().asFile,
            cachesDir = parameters.cachesDir.get().asFile,
            languageVersion = parameters.languageVersion.get(),
            apiVersion = parameters.apiVersion.get(),
            nativeTarget = parameters.nativeTarget.get(),
        )
    }
}
