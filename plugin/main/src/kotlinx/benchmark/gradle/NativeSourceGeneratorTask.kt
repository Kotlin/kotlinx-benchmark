package kotlinx.benchmark.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.konan.library.KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
import org.jetbrains.kotlin.konan.util.KlibMetadataFactories
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.createKotlinLibraryComponents
import org.jetbrains.kotlin.library.metadata.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.library.resolver.impl.libraryResolver
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.util.Logger
import java.io.File
import javax.inject.Inject

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
        workerExecutor.submit(NativeSourceGeneratorWorker::class.java) { config ->
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

private val Builtins = DefaultBuiltIns.Instance
private val NativeFactories = KlibMetadataFactories( { Builtins }, NullFlexibleTypeDeserializer)

class NativeSourceGeneratorWorker
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

        val logger = object : Logger {
            override fun log(message: String) {}
            override fun error(message: String) = kotlin.error("e: $message")
            override fun warning(message: String) {}
            override fun fatal(message: String) = kotlin.error("e: $message")
        }
        val pathResolver = SeveralKlibComponentResolver(dependencyPaths.map { it.canonicalPath }, listOf(KotlinAbiVersion.CURRENT), logger)
        val libraryResolver = pathResolver.libraryResolver()

        val factory = NativeFactories.DefaultDeserializedDescriptorFactory

        val konanFile = org.jetbrains.kotlin.konan.file.File(lib.canonicalPath)
        val library = resolveSingleFileKlib(konanFile)

        val versionSpec = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE)
        val storageManager = LockBasedStorageManager("Inspect")

        val module = factory.createDescriptorOptionalBuiltIns(
            library,
            versionSpec,
            storageManager,
            Builtins,
            null,
            LookupTracker.DO_NOTHING
        )

        val dependencies = libraryResolver.resolveWithDependencies(library.unresolvedDependencies)
        val dependenciesResolved = NativeFactories.DefaultResolvedDescriptorsFactory.createResolved(
            dependencies,
            storageManager,
            Builtins,
            versionSpec,
            null,
            emptyList()
        )

        val dependenciesDescriptors = dependenciesResolved.resolvedDescriptors
        val forwardDeclarationsModule = dependenciesResolved.forwardDeclarationsModule

        module.setDependencies(listOf(module) + dependenciesDescriptors + forwardDeclarationsModule)
        return module
    }

    private class SeveralKlibComponentResolver(
        klibFiles: List<String>,
        knownAbiVersions: List<KotlinAbiVersion>?,
        logger: Logger
    ) : KotlinLibraryProperResolverWithAttributes<KotlinLibrary>(
        emptyList(), klibFiles, knownAbiVersions, emptyList(),
        null, null, false, logger, listOf(KLIB_INTEROP_IR_PROVIDER_IDENTIFIER)
    ) {
        override fun libraryComponentBuilder(
            file: org.jetbrains.kotlin.konan.file.File,
            isDefault: Boolean
        ): List<KotlinLibrary> =
            createKotlinLibraryComponents(file, isDefault)
    }
}