package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.workers.*
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.storage.*
import java.io.*
import javax.inject.*

@CacheableTask
open class NativeSourceGeneratorTask
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

    @TaskAction
    fun generate() {
        val workQueue = workerExecutor.classLoaderIsolation()
        workQueue.submit(NativeSourceGeneratorWorker::class.java) { parameters ->
            parameters.title = title
            parameters.target = nativeTarget
            parameters.inputClassesDirs = inputClassesDirs.files
            parameters.inputDependencies = inputDependencies.files
            parameters.outputSourcesDir = outputSourcesDir
            parameters.outputResourcesDir = outputResourcesDir
        }
        workQueue.await()
    }
}

@KotlinxBenchmarkPluginInternalApi
interface NativeSourceGeneratorWorkerParameters : WorkParameters {
    var title: String
    var target: String
    var inputClassesDirs: Set<File>
    var inputDependencies: Set<File>
    var outputSourcesDir: File
    var outputResourcesDir: File
}

@KotlinxBenchmarkPluginInternalApi
abstract class NativeSourceGeneratorWorker : WorkAction<NativeSourceGeneratorWorkerParameters> {
    override fun execute() {
        cleanup(parameters.outputSourcesDir)
        cleanup(parameters.outputResourcesDir)
        parameters.inputClassesDirs
            .filter { it.exists() && it.name.endsWith(KLIB_FILE_EXTENSION_WITH_DOT) }
            .forEach { lib ->
                if (parameters.target.isEmpty())
                    throw Exception("nativeTarget should be specified for API generator for native targets")

                val storageManager = LockBasedStorageManager("Inspect")
                val module = KlibResolver.Native.createModuleDescriptor(lib, parameters.inputDependencies, storageManager)
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
