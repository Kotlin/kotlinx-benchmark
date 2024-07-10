package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.internal.generator.RequiresKotlinCompilerEmbeddable
import kotlinx.benchmark.gradle.internal.generator.workers.GenerateWasmSourceWorker
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class WasmSourceGeneratorTask
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

    @get:Classpath
    abstract val runtimeClasspath: ConfigurableFileCollection

    @TaskAction
    fun generate() {
        val workQueue = workerExecutor.classLoaderIsolation {
            it.classpath.from(runtimeClasspath)
        }

        @OptIn(RequiresKotlinCompilerEmbeddable::class)
        workQueue.submit(GenerateWasmSourceWorker::class.java) {
            it.title.set(title)
            it.inputClasses.from(inputClassesDirs)
            it.inputDependencies.from(inputDependencies)
            it.outputSourcesDir.set(outputSourcesDir)
            it.outputResourcesDir.set(outputResourcesDir)
        }

        workQueue.await() // I'm not sure if waiting is necessary,
        // but I suspect that the task dependencies aren't configured correctly,
        // so: better-safe-than-sorry.
        // Try removing await() when Benchmarks follows Gradle best practices.
    }
}
