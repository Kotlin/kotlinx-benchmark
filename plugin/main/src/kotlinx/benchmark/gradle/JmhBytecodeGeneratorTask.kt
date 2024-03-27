package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
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

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var inputClassesDirs: FileCollection

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var inputCompileClasspath: FileCollection

    @OutputDirectory
    lateinit var outputResourcesDir: File

    @OutputDirectory
    lateinit var outputSourcesDir: File

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var runtimeClasspath: FileCollection

    @Optional
    @Input
    var executableProvider: Provider<String> = project.provider { null }

    @TaskAction
    fun generate() {
        val workQueue = workerExecutor.processIsolation { workerSpec ->
            workerSpec.classpath.setFrom(runtimeClasspath.files)
            if (executableProvider.isPresent) {
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
