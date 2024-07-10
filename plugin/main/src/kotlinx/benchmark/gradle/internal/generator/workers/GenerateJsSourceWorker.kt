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
                inputDependencies = parameters.inputDependencies.files,
                outputSourcesDir = parameters.outputSourcesDir.get().asFile,
                useBenchmarkJs = parameters.useBenchmarkJs.get(),
            )
        }
    }

    private fun generateSources(
        title: String,
        lib: File,
        inputDependencies: Set<File>,
        outputSourcesDir: File,
        useBenchmarkJs: Boolean,
    ) {
        val modules = loadIr(
            lib = lib,
            inputDependencies = inputDependencies,
            storageManager = LockBasedStorageManager("Inspect"),
        )
        modules.forEach { module ->
            val generator = SuiteSourceGenerator(
                title,
                module,
                outputSourcesDir,
                if (useBenchmarkJs) Platform.JsBenchmarkJs else Platform.JsBuiltIn
            )
            generator.generate()
        }
    }

    private fun loadIr(
        lib: File,
        inputDependencies: Set<File>,
        storageManager: StorageManager,
    ): List<ModuleDescriptor> {
        // skip processing of empty dirs (fails if not to do it)
        if (lib.listFiles() == null) return emptyList()
        val dependencies = inputDependencies.filterNot { it.extension == "js" }.toSet()
        val module = KlibResolver.JS.createModuleDescriptor(lib, dependencies, storageManager)
        return listOf(module)
    }
}
