package org.jetbrains.gradle.benchmarks

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
    @InputFiles
    @PathSensitive(PathSensitivity.ABSOLUTE)
    lateinit var inputClassesDirs: FileCollection

    @OutputDirectory
    lateinit var outputResourcesDir: File

    @OutputDirectory
    lateinit var outputSourcesDir: File

    @InputFiles
    @PathSensitive(PathSensitivity.ABSOLUTE)
    lateinit var runtimeClasspath: FileCollection
    
    @TaskAction
    fun generate() {
        println("Worker classpath: ${runtimeClasspath.files}")
        workerExecutor.submit(JmhBytecodeGeneratorWorker::class.java) { config ->
            config.isolationMode = IsolationMode.CLASSLOADER
            config.classpath = runtimeClasspath
            config.params(inputClassesDirs.files, outputSourcesDir, outputResourcesDir)
        }
    }
}
