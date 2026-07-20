package kotlinx.benchmark.gradle.internal.generator.workers

import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.processing.KSPNativeConfig
import com.google.devtools.ksp.processing.KspGradleLogger
import kotlinx.benchmark.gradle.Platform
import kotlinx.benchmark.gradle.internal.generator.BenchmarkSymbolProcessorProvider
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

/**
 * Generates Native benchmarking source code with KSP.
 *
 * @see kotlinx.benchmark.gradle.NativeSourceGeneratorWithKSPTask
 */
internal abstract class GenerateNativeSourceWithKSPWorker : WorkAction<GenerateNativeSourceWithKSPWorker.Params> {

    internal interface Params : WorkParameters {
        val title: Property<String>
        val target: Property<String>
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
    }

    override fun execute() {
        parameters.outputSourcesDir.get().asFile.deleteRecursively()
        parameters.outputResourcesDir.get().asFile.deleteRecursively()

        val kspConfig = KSPNativeConfig.Builder().apply {
            moduleName = parameters.title.get()
            sourceRoots = parameters.sourceRoots.files.toList()
            libraries = parameters.inputDependencies.files.toList()
            friends = emptyList()
            projectBaseDir = parameters.projectDir.asFile.get()
            outputBaseDir = parameters.outputBaseDir.asFile.get()
            cachesDir = parameters.cachesDir.asFile.get()
            classOutputDir = parameters.outputClassesDir.asFile.get()
            kotlinOutputDir = parameters.outputSourcesDir.asFile.get()
            resourceOutputDir = parameters.outputResourcesDir.asFile.get()
            languageVersion = parameters.languageVersion.get()
            apiVersion = parameters.apiVersion.get()
            target = parameters.target.get()
        }.build()

        val exitCode = KotlinSymbolProcessing(
            kspConfig,
            listOf(BenchmarkSymbolProcessorProvider(parameters.title.get(), Platform.NativeBuiltIn)),
            KspGradleLogger(KspGradleLogger.LOGGING_LEVEL_LOGGING)
        ).execute()
        if (exitCode != KotlinSymbolProcessing.ExitCode.OK) {
            throw GradleException("Error running KSP: $exitCode")
        }
    }
}