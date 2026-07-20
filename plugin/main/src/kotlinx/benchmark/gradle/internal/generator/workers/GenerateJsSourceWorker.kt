package kotlinx.benchmark.gradle.internal.generator.workers

import kotlinx.benchmark.gradle.internal.generator.RequiresKotlinCompilerEmbeddable
import kotlinx.benchmark.klib.KlibModule
import kotlinx.benchmark.klib.KotlinxBenchmarkCodegenInternalApi
import kotlinx.benchmark.klib.Platform
import kotlinx.benchmark.klib.SuiteSourceGenerator
import org.gradle.api.file.*
import org.gradle.api.provider.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
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

    @OptIn(KotlinxBenchmarkCodegenInternalApi::class)
    private fun generateSources(
        title: String,
        lib: File,
        outputSourcesDir: File,
        useBenchmarkJs: Boolean,
    ) {
        val modules = KlibModule.loadWebModules(lib)
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


}
