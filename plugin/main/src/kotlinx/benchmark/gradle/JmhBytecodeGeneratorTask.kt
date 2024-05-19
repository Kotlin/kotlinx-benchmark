package kotlinx.benchmark.gradle

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.workers.*
import java.io.*
import javax.inject.*

@Suppress("UnstableApiUsage")
@CacheableTask
open class JmhBytecodeGeneratorTask
@Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {

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
    
    @TaskAction
    fun generate() {
        val workQueue = workerExecutor.processIsolation { workerSpec ->
            workerSpec.classpath.setFrom(runtimeClasspath.files)
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
