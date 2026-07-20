package kotlinx.benchmark.gradle.internal.generator.workers

import kotlinx.benchmark.gradle.internal.generator.RequiresKotlinCompilerEmbeddable
import kotlinx.benchmark.klib.KlibModule
import kotlinx.benchmark.klib.Platform
import kotlinx.benchmark.klib.SuiteSourceGenerator
import org.gradle.api.file.*
import org.gradle.api.provider.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
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
        val modules = KlibModule.loadWebModules(lib)
        modules.forEach { module ->
            val generator = SuiteSourceGenerator(
                title,
                module,
                outputSourcesDir,
                Platform.WasmBuiltIn
            )
            generator.generate()
        }
    }
}
