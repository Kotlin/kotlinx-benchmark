package kotlinx.benchmark.gradle

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.workers.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.properties.*
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.util.*
import org.jetbrains.kotlin.storage.*
import java.io.*
import java.nio.file.*
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
        workerExecutor.submit(NativeBytecodeGeneratorWorker::class.java) { config ->
            config.isolationMode = IsolationMode.CLASSLOADER
            //config.classpath = runtimeClasspath
            config.params(
                title,
                nativeTarget,
                inputClassesDirs.files,
                inputDependencies.files,
                outputSourcesDir,
                outputResourcesDir
            )
        }
        workerExecutor.await()
    }
}

class NativeBytecodeGeneratorWorker
@Inject constructor(
    private val title: String,
    private val target: String,
    private val inputClassesDirs: Set<File>,
    private val inputDependencies: Set<File>,
    private val outputSourcesDir: File,
    private val outputResourcesDir: File
) : Runnable {
    override fun run() {
        cleanup(outputSourcesDir)
        cleanup(outputResourcesDir)

        inputClassesDirs
            .filter { it.exists() && it.name.endsWith(KLIB_FILE_EXTENSION_WITH_DOT) }
            .forEach { lib ->
                val module = createModuleDescriptor(target, lib, inputDependencies)
                val generator = SuiteSourceGenerator(
                    title,
                    module,
                    outputSourcesDir,
                    Platform.NATIVE
                )
                generator.generate()
            }
    }


    private fun createModuleDescriptor(nativeTarget: String, lib: File, dependencyPaths: Set<File>): ModuleDescriptor {
        if (nativeTarget.isEmpty())
            throw Exception("nativeTarget should be specified for API generator for native targets")

        val konanTarget = PredefinedKonanTargets.getByName(nativeTarget)!!
        val versionSpec = LanguageVersionSettingsImpl(
            LanguageVersion.LATEST_STABLE,
            ApiVersion.LATEST_STABLE
        )
        val ABI_VERSION = 8

        val pathResolver = ProvidedPathResolver(dependencyPaths, konanTarget)
        val libraryResolver = pathResolver.libraryResolver(ABI_VERSION)

        val factory = KonanFactories.DefaultDeserializedDescriptorFactory

        val konanFile = org.jetbrains.kotlin.konan.file.File(lib.canonicalPath)

        val library = createKonanLibrary(konanFile, ABI_VERSION, konanTarget, false)
        val unresolvedDependencies = library.manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS)
        val storageManager = LockBasedStorageManager("Inspect")

        val module = factory.createDescriptorAndNewBuiltIns(library, versionSpec, storageManager)

        val dependencies = libraryResolver.resolveWithDependencies(unresolvedDependencies)
        val dependenciesResolved = KonanFactories.DefaultResolvedDescriptorsFactory.createResolved(
            dependencies,
            storageManager,
            null,
            versionSpec
        )

        val dependenciesDescriptors = dependenciesResolved.resolvedDescriptors
        val forwardDeclarationsModule = dependenciesResolved.forwardDeclarationsModule

        module.setDependencies(listOf(module) + dependenciesDescriptors + forwardDeclarationsModule)
        return module
    }

    private class ProvidedPathResolver(private val dependencies: Set<File>, override val target: KonanTarget) :
        SearchPathResolverWithTarget {

        override val searchRoots: List<org.jetbrains.kotlin.konan.file.File> get() = emptyList()

        private val nameMap = dependencies
            // TODO: what's wrong with JARs? They seem common libs, how does native ignores them?
            .filter { it.extension != "jar" }
            .map {
                val file = org.jetbrains.kotlin.konan.file.File(it.absolutePath)
                // Need to load library to know its uniqueName, some libs like atomicfu has it different from klib file name
                createKonanLibrary(file, 1, target)
            }
            .associateBy { it.uniqueName }
            .mapValues { it.value.libraryFile }

        override fun resolve(givenPath: String): org.jetbrains.kotlin.konan.file.File {
            val path = Paths.get(givenPath)
            return when {
                path.isAbsolute -> org.jetbrains.kotlin.konan.file.File(path)
                else -> {
                    val file = nameMap[givenPath]
                    if (file != null)
                        return file

                    println("Cannot resolve library $givenPath with the following dependencies:")
                    println(dependencies.joinToString(prefix = "  ", separator = "\n  "))
                    throw Exception("Cannot resolve library '$givenPath' with $nameMap")
                }
            }
        }

        override fun defaultLinks(
            noStdLib: Boolean,
            noDefaultLibs: Boolean
        ): List<org.jetbrains.kotlin.konan.file.File> =
            emptyList()
    }
}