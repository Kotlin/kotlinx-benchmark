package kotlinx.benchmark.gradle.internal.generator.workers

import kotlinx.benchmark.gradle.KlibResolver
import kotlinx.benchmark.gradle.Platform
import kotlinx.benchmark.gradle.SuiteSourceGenerator
import kotlinx.benchmark.gradle.createModuleDescriptor
import kotlinx.benchmark.gradle.internal.generator.RequiresKotlinCompilerEmbeddable
import org.gradle.api.file.*
import org.gradle.api.provider.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
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
                inputDependencies = inputDependencies,
                outputSourcesDir = outputSourcesDir,
            )
        }
    }

    private fun generateSources(
        title: String,
        lib: File,
        inputDependencies: Set<File>,
        outputSourcesDir: File,
    ) {
        val modules = loadIr(
            lib,
            inputDependencies = inputDependencies,
            LockBasedStorageManager("Inspect"),
        )
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

    private fun loadIr(
        lib: File,
        inputDependencies: Set<File>,
        storageManager: StorageManager,
    ): List<ModuleDescriptor> {
        //skip processing of empty dirs (fail if not to do it)
        if (lib.listFiles() == null) return emptyList()
        val dependencies = inputDependencies.filterNot { it.extension == "js" }.toSet()
        val module = KlibResolver.JS.createModuleDescriptor(lib, dependencies, storageManager)
        return listOf(module)
    }
}
