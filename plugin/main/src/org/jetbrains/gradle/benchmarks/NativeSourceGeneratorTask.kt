package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.logging.*
import org.gradle.api.tasks.*
import org.gradle.workers.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.util.*
import org.jetbrains.kotlin.konan.util.KonanFactories.DefaultDeserializedDescriptorFactory
import org.jetbrains.kotlin.storage.*
import java.io.*
import java.nio.file.*
import javax.inject.*

@Suppress("UnstableApiUsage")
@CacheableTask
open class NativeSourceGeneratorTask
@Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {
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
    lateinit var target: String

    @TaskAction
    fun generate() {
        cleanup(outputSourcesDir)
        cleanup(outputResourcesDir)

        val konanTarget = PredefinedKonanTargets.getByName(target)!!
        val versionSpec = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE)
        val ABI_VERSION = 1

        val pathResolver = ProvidedPathResolver(logger, inputDependencies.files, konanTarget)
        val libraryResolver = pathResolver.libraryResolver(ABI_VERSION)

        val factory = DefaultDeserializedDescriptorFactory
        inputClassesDirs.files.filter { it.name.endsWith(KLIB_FILE_EXTENSION_WITH_DOT) }.forEach { lib ->
            val konanFile = org.jetbrains.kotlin.konan.file.File(lib.canonicalPath)

            val library = createKonanLibrary(konanFile, ABI_VERSION, konanTarget, false)
            val unresolvedDependencies = library.unresolvedDependencies
            val storageManager = LockBasedStorageManager()

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
            val generator = SuiteSourceGenerator(module, outputSourcesDir, Platform.NATIVE)
            generator.generate()
        }
    }


}

class ProvidedPathResolver(
    private val logger: Logger,
    private val dependencies: MutableSet<File>,
    override val target: KonanTarget
) : SearchPathResolverWithTarget {

    override val searchRoots: List<org.jetbrains.kotlin.konan.file.File> get() = emptyList()

    private val shortMap = dependencies
        .map { org.jetbrains.kotlin.konan.file.File(it.absolutePath) }
        .associateBy { it.name.removeSuffix(KLIB_FILE_EXTENSION_WITH_DOT) }

    override fun resolve(givenPath: String): org.jetbrains.kotlin.konan.file.File {
        val path = Paths.get(givenPath)
        return when {
            path.isAbsolute -> org.jetbrains.kotlin.konan.file.File(path)
            else -> {
                val file = shortMap[givenPath]
                if (file != null)
                    return file

                logger.error("Cannot resolve library $givenPath with the following dependencies:")
                logger.error(dependencies.joinToString(prefix = "  ", separator = "\n  "))
                throw Exception("Cannot resolve library '$givenPath' with $shortMap")
            }
        }
    }

    override fun defaultLinks(noStdLib: Boolean, noDefaultLibs: Boolean): List<org.jetbrains.kotlin.konan.file.File> =
        emptyList()
}