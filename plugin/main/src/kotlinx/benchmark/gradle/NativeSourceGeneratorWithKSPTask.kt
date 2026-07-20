package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.internal.generator.workers.GenerateNativeSourceWithKSPWorker
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class NativeSourceGeneratorWithKSPTask
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

    @OutputDirectory
    lateinit var outputClassesDir: File

    @Internal
    lateinit var projectDir: File

    @Internal
    lateinit var outputBaseDir: File

    @LocalState
    lateinit var cachesDir: File

    @get:Input
    abstract val languageVersion: Property<String>

    @get:Input
    abstract val apiVersion: Property<String>

    @Input
    lateinit var nativeTarget: String

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceRoots: ConfigurableFileCollection

    @TaskAction
    fun generate() {
        val workQueue = workerExecutor.noIsolation()

        workQueue.submit(GenerateNativeSourceWithKSPWorker::class.java) {
            it.title.set(title)
            it.target.set(nativeTarget)
            it.sourceRoots.from(sourceRoots)
            it.inputDependencies.from(inputDependencies)
            it.outputSourcesDir.set(outputSourcesDir)
            it.outputResourcesDir.set(outputResourcesDir)
            it.outputClassesDir.set(outputClassesDir)
            it.projectDir.set(projectDir)
            it.outputBaseDir.set(outputBaseDir)
            it.cachesDir.set(cachesDir)
            it.languageVersion.set(languageVersion)
            it.apiVersion.set(apiVersion)
        }

        workQueue.await() // I'm not sure if waiting is necessary,
        // but I suspect that the task dependencies aren't configured correctly,
        // so: better-safe-than-sorry.
        // Try removing await() when Benchmarks follows Gradle best practices.
    }
}
