package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.gradle.api.file.*
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
import java.util.stream.*
import javax.inject.*

@Suppress("UnstableApiUsage")
@CacheableTask
open class NativeSourceGeneratorTask
@Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var inputClassesDirs: FileCollection

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
        val konanHome = Paths.get(System.getenv("HOME"), ".konan", "kotlin-native-macos-1.0.3")
        val versionSpec = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE)
        val ABI_VERSION = 1

        val pathResolver = MySearchPathResolverWithTarget(konanHome, konanTarget)
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

class MySearchPathResolverWithTarget(
    private val konanHome: Path,
    override val target: KonanTarget
) : SearchPathResolverWithTarget {

    val commonRoot = org.jetbrains.kotlin.konan.file.File(konanHome.resolve("klib").resolve("common"))
    val platformPath = konanHome.resolve("klib").resolve("platform").resolve(target.name)
    val platformRoot = org.jetbrains.kotlin.konan.file.File(platformPath)

    override val searchRoots: List<org.jetbrains.kotlin.konan.file.File> get() = listOf(commonRoot, platformRoot)

    override fun resolve(givenPath: String): org.jetbrains.kotlin.konan.file.File {
        val path = Paths.get(givenPath)
        return when {
            path.isAbsolute -> org.jetbrains.kotlin.konan.file.File(path)
            else -> {
                val commonLib = commonRoot.child(givenPath)
                if (commonLib.exists) {
                    return commonLib
                }

                val platformLib = platformRoot.child(givenPath)
                if (platformLib.exists)
                    return platformLib

                throw Exception("Cannot resolve library with $commonRoot and $platformRoot: $givenPath")
            }
        }
    }

    override fun defaultLinks(noStdLib: Boolean, noDefaultLibs: Boolean): List<org.jetbrains.kotlin.konan.file.File> {
        val list = mutableListOf<org.jetbrains.kotlin.konan.file.File>()

        if (!noStdLib) {
            list += commonRoot.child("stdlib")
        }

        if (!noDefaultLibs) {
            Files
                .list(platformPath)
                .map { org.jetbrains.kotlin.konan.file.File(it) }
                .collect(Collectors.toCollection { list })
        }

        return list
    }
}