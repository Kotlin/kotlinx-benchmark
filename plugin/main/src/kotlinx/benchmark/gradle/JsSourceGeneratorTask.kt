package kotlinx.benchmark.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import java.io.File
import javax.inject.Inject

@Suppress("UnstableApiUsage")
@CacheableTask
open class JsSourceGeneratorTask
@Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {
    @Input
    lateinit var title: String

    @Input
    var ir: Boolean = false

    @Input
    var useBenchmarkJs: Boolean = true

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var inputClassesDirs: FileCollection

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var inputDependencies: FileCollection

    @OutputDirectory
    lateinit var outputResourcesDir: File

    @OutputDirectory
    lateinit var outputSourcesDir: File

    @TaskAction
    fun generate() {
        cleanup(outputSourcesDir)
        cleanup(outputResourcesDir)

        inputClassesDirs.files.forEach { lib: File ->
            generateSources(lib)
        }
    }

    private fun generateSources(lib: File) {
        val modules = load(lib)
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

    private fun load(lib: File): List<ModuleDescriptor> {
        val storageManager = LockBasedStorageManager("Inspect")
    
        // skip processing of empty dirs (fail if not to do it)
        if (lib.listFiles() == null) return emptyList()
        val dependencies = inputDependencies.files.filterNot { it.extension == "js" }.toSet()
        val module = KlibResolver.JS.createModuleDescriptor(lib, dependencies, storageManager)
        return listOf(module)
    }
}