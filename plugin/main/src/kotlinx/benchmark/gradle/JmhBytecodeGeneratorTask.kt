package kotlinx.benchmark.gradle

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
public open class JmhBytecodeGeneratorTask
@Inject
internal constructor(
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {


    @Classpath
    public lateinit var inputClassesDirs: FileCollection

    @Classpath
    public lateinit var inputCompileClasspath: FileCollection

    @OutputDirectory
    public lateinit var outputResourcesDir: File

    @OutputDirectory
    public lateinit var outputSourcesDir: File

    @Classpath
    public lateinit var runtimeClasspath: FileCollection

    @Optional
    @Nested
    public val javaLauncher: Property<JavaLauncher> = project.objects.property(JavaLauncher::class.java)

    @Deprecated(message = "Use jvmLauncher property instead.", level = DeprecationLevel.ERROR)
    @Optional
    @Input
    public var executableProvider: Provider<String> = project.provider { null }

    @TaskAction
    public fun generate() {
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
