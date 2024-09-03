package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.internal.generator.RequiresKotlinCompilerEmbeddable
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION_WITH_DOT
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class NativeSourceGeneratorTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor(
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {

    @Input
    lateinit var title: String

    @Classpath
    lateinit var inputClassesDirs: FileCollection

    @Classpath
    lateinit var inputDependencies: FileCollection

    @OutputDirectory
    lateinit var outputResourcesDir: File

    @OutputDirectory
    lateinit var outputSourcesDir: File

    @Input
    lateinit var nativeTarget: String

    @get:Classpath
    abstract val runtimeClasspath: ConfigurableFileCollection

    @TaskAction
    fun generate() {
        val workQueue = workerExecutor.classLoaderIsolation {
            it.classpath.from(runtimeClasspath)
        }

        @OptIn(RequiresKotlinCompilerEmbeddable::class)
        workQueue.submit(NativeSourceGeneratorWorker::class.java) {
            it.title = title
            it.target = nativeTarget
            it.inputClassesDirs = inputClassesDirs.files
            it.inputDependencies = inputDependencies.files
            it.outputSourcesDir = outputSourcesDir
            it.outputResourcesDir = outputResourcesDir
        }

        workQueue.await() // I'm not sure if waiting is necessary,
        // but I suspect that the task dependencies aren't configured correctly,
        // so: better-safe-than-sorry.
        // Try removing await() when Benchmarks follows Gradle best practices.
    }
}

@KotlinxBenchmarkPluginInternalApi
// TODO https://github.com/Kotlin/kotlinx-benchmark/issues/211
//      Replace NativeSourceGeneratorWorkerParameters with NativeSourceGeneratorWorker.Parameters,
//      so that it is like the other workers.
interface NativeSourceGeneratorWorkerParameters : WorkParameters {
    var title: String
    var target: String
    var inputClassesDirs: Set<File>
    var inputDependencies: Set<File>
    var outputSourcesDir: File
    var outputResourcesDir: File
}

@KotlinxBenchmarkPluginInternalApi
@RequiresKotlinCompilerEmbeddable
// TODO https://github.com/Kotlin/kotlinx-benchmark/issues/211
//      Change visibility of NativeSourceGeneratorWorker to `internal`
//      Move to package kotlinx.benchmark.gradle.internal.generator.workers, alongside the other workers.
abstract class NativeSourceGeneratorWorker : WorkAction<NativeSourceGeneratorWorkerParameters> {

    // TODO https://github.com/Kotlin/kotlinx-benchmark/issues/211
    //      replace NativeSourceGeneratorWorkerParameters with this interface:
    //internal interface Parameters : WorkParameters {
    //    val title: Property<String>
    //    val target: Property<String>
    //    val inputClassesDirs: ConfigurableFileCollection
    //    val inputDependencies: ConfigurableFileCollection
    //    val outputSourcesDir: DirectoryProperty
    //    val outputResourcesDir: DirectoryProperty
    //}

    override fun execute() {
        parameters.outputSourcesDir.deleteRecursively()
        parameters.outputResourcesDir.deleteRecursively()
        parameters.inputClassesDirs
            .filter { it.exists() && it.name.endsWith(KLIB_FILE_EXTENSION_WITH_DOT) }
            .forEach { lib ->
                if (parameters.target.isEmpty())
                    throw Exception("nativeTarget should be specified for API generator for native targets")

                val storageManager = LockBasedStorageManager("Inspect")
                val module =
                    KlibResolver.Native.createModuleDescriptor(lib, parameters.inputDependencies, storageManager)
                val generator = SuiteSourceGenerator(
                    parameters.title,
                    module,
                    parameters.outputSourcesDir,
                    Platform.NativeBuiltIn
                )
                generator.generate()
            }
    }
}
