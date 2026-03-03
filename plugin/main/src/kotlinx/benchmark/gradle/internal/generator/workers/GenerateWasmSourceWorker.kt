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
 * Generates Wasm benchmarking source code.
 *
 * This worker requires `kotlin-compiler-embeddable` and *must* be run in an isolated classpath.
 *
 * @see kotlinx.benchmark.gradle.WasmSourceGeneratorTask
 */
@RequiresKotlinCompilerEmbeddable
internal abstract class GenerateWasmSourceWorker : WorkAction<GenerateWasmSourceWorker.Params> {

    internal interface Params : WorkParameters {
        val title: Property<String>
        val inputClasses: ConfigurableFileCollection
        val inputDependencies: ConfigurableFileCollection
        val outputSourcesDir: DirectoryProperty
        val outputResourcesDir: DirectoryProperty
    }

    override fun execute() {

        val title = parameters.title.get()
        val inputDependencies = parameters.inputDependencies.files
        val outputSourcesDir = parameters.outputSourcesDir.get().asFile

        parameters.outputSourcesDir.get().asFile.deleteRecursively()
        parameters.outputResourcesDir.get().asFile.deleteRecursively()

        parameters.inputClasses.forEach { lib: File ->
            generateSources(
                title = title,
                lib = lib,
                outputSourcesDir = outputSourcesDir,
            )
        }
    }

    private fun generateSources(
        title: String,
        lib: File,
        outputSourcesDir: File,
    ) {
        val metadata = KlibMetadataLoaderFactory.create().load(lib)

        val generator = SuiteSourceGenerator(
            title,
            metadata,
            outputSourcesDir,
            Platform.WasmBuiltIn
        )
        generator.generate()
    }
}
