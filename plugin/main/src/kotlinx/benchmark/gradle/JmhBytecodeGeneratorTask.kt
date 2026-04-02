package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.workers.*
import java.io.*
import javax.inject.*

@CacheableTask
open class JmhBytecodeGeneratorTask
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor(
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {


    @Classpath
    lateinit var inputClassesDirs: FileCollection

    @Classpath
    lateinit var inputCompileClasspath: FileCollection

    @OutputDirectory
    lateinit var outputResourcesDir: File

    @OutputDirectory
    lateinit var outputSourcesDir: File

    @Classpath
    lateinit var runtimeClasspath: FileCollection

    @Optional
    @Nested
    val javaLauncher: Property<JavaLauncher> = project.objects.property(JavaLauncher::class.java)

    @Deprecated(message = "Use jvmLauncher property instead.", level = DeprecationLevel.ERROR)
    @Optional
    @Input
    var executableProvider: Provider<String> = project.provider { null }

    @TaskAction
    fun generate() {
        val workQueue = workerExecutor.processIsolation { workerSpec ->
            workerSpec.classpath.setFrom(runtimeClasspath.files)
            @Suppress("DEPRECATION_ERROR")
            if (javaLauncher.isPresent) {
                workerSpec.forkOptions.executable = javaLauncher.get().executablePath.asFile.absolutePath
            } else if (executableProvider.isPresent) {
                workerSpec.forkOptions.executable = executableProvider.get()
            }
        }
        workQueue.submit(JmhBytecodeGeneratorWorker::class.java) { workParameters ->
            workParameters.inputClasses.setFrom(inputClassesDirs.files)
            workParameters.inputClasspath.setFrom(inputCompileClasspath.files)
            workParameters.outputSourceDirectory.set(outputSourcesDir)
            workParameters.outputResourceDirectory.set(outputResourcesDir)
        }
        workQueue.await()
    }
}
