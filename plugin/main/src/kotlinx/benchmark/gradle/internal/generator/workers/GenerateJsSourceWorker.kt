package kotlinx.benchmark.gradle.internal.generator.workers

import kotlinx.benchmark.gradle.Platform
import kotlinx.benchmark.gradle.SuiteSourceGenerator
import kotlinx.benchmark.gradle.internal.generator.RequiresKotlinCompilerEmbeddable
import kotlinx.benchmark.klib.KlibMetadataLoaderFactory
import kotlinx.metadata.klib.KlibModuleMetadata
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import java.io.File

/**
 * Generates JavaScript benchmarking source code.
 *
 * This worker requires `kotlin-compiler-embeddable` and *must* be run in an isolated classpath.
 *
 * @see kotlinx.benchmark.gradle.JsSourceGeneratorTask
 */
@RequiresKotlinCompilerEmbeddable
internal abstract class GenerateJsSourceWorker : WorkAction<GenerateJsSourceWorker.Params> {

    internal interface Params : WorkParameters {
        val title: Property<String>
        val inputClasses: ConfigurableFileCollection
        val inputDependencies: ConfigurableFileCollection
        val outputSourcesDir: DirectoryProperty
        val outputResourcesDir: DirectoryProperty
        val useBenchmarkJs: Property<Boolean>
    }

    override fun execute() {
        parameters.outputSourcesDir.get().asFile.deleteRecursively()
        parameters.outputResourcesDir.get().asFile.deleteRecursively()

        parameters.inputClasses.forEach { lib: File ->
            generateSources(
                title = parameters.title.get(),
                lib = lib,
                outputSourcesDir = parameters.outputSourcesDir.get().asFile,
                useBenchmarkJs = parameters.useBenchmarkJs.get(),
            )
        }
    }

    private fun generateSources(
        title: String,
        lib: File,
        outputSourcesDir: File,
        useBenchmarkJs: Boolean,
    ) {
        val metadata = KlibMetadataLoaderFactory.create().load(lib)
        val generator = SuiteSourceGenerator(
            title,
            metadata,
            outputSourcesDir,
            if (useBenchmarkJs) Platform.JsBenchmarkJs else Platform.JsBuiltIn
        )
        generator.generate()
    }
}
