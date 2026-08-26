package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.generator.Platform
import kotlinx.benchmark.gradle.internal.generator.workers.GenerateSourceWorker
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

/**
 * Base task responsible for generating Native, Js and Wasm sources.
 *
 * Majority of properties mimics [KspGradleConfig](https://github.com/google/ksp/blob/78828d47f3051a5af34a5358e5d2a55b422d1a94/gradle-plugin/src/main/kotlin/com/google/devtools/ksp/gradle/KspAATask.kt#L588).
 */
internal abstract class SourceGeneratorTask
@Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {

    @get:Input
    abstract val title: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceRoots: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputDependencies: ConfigurableFileCollection

    @get:Classpath
    abstract val runtimeClasspath: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputResourcesDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputSourcesDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputClassesDir: DirectoryProperty

    @get:Internal
    abstract val outputBaseDir: DirectoryProperty

    @get:Internal
    abstract val projectDir: DirectoryProperty

    @get:LocalState
    abstract val cachesDir: DirectoryProperty

    @get:Input
    abstract val languageVersion: Property<String>

    @get:Input
    abstract val apiVersion: Property<String>

    @get:Input
    abstract val platform: Property<Platform>

    fun setupOutputDirectories(benchmarksBuildDirectory: File) {
        outputBaseDir.set(benchmarksBuildDirectory)
        outputClassesDir.set(outputBaseDir.dir("classes"))
        outputResourcesDir.set(outputBaseDir.dir("resources"))
        outputSourcesDir.set(outputBaseDir.dir("sources"))
        cachesDir.set(outputBaseDir.dir("kspCaches"))
    }

    @TaskAction
    fun generate() {
        val workQueue = workerExecutor.classLoaderIsolation {
            it.classpath.from(runtimeClasspath)
        }
        workQueue.submit(GenerateSourceWorker::class.java) {
            it.title.set(title)
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
            it.platform.set(platform)
            it.nativeTarget.set(nativeTargetName())
        }
        workQueue.await()
    }

    protected open fun nativeTargetName(): String = ""
}
