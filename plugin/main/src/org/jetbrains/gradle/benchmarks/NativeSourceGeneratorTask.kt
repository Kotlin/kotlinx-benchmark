package org.jetbrains.gradle.benchmarks

import kotlinx.team.infra.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.workers.*
import org.jetbrains.kotlin.konan.library.*
import java.io.*
import javax.inject.*

@Suppress("UnstableApiUsage")
@CacheableTask
open class NativeSourceGeneratorTask
@Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {
    @Input
    lateinit var title: String

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var inputClassesDirs: FileCollection

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var inputDependencies: FileCollection

    @OutputDirectory
    lateinit var outputResourcesDir: File

    @OutputDirectory
    lateinit var outputSourcesDir: File

    @Input
    lateinit var nativeTarget: String

    @TaskAction
    fun generate() {
        cleanup(outputSourcesDir)
        cleanup(outputResourcesDir)

        inputClassesDirs.files
            .filter { it.exists() && it.name.endsWith(KLIB_FILE_EXTENSION_WITH_DOT) }
            .forEach { lib ->
                val module = project.createModuleDescriptor(nativeTarget, lib, inputDependencies.files)
                val generator = SuiteSourceGenerator(title, module, outputSourcesDir, Platform.NATIVE)
                generator.generate()
            }

    }
}