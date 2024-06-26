package kotlinx.benchmark.gradle

import org.jetbrains.kotlin.backend.common.serialization.metadata.*
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.*
import org.jetbrains.kotlin.library.metadata.*
import org.jetbrains.kotlin.library.metadata.resolver.impl.*
import org.jetbrains.kotlin.storage.*
import org.jetbrains.kotlin.util.*
import java.io.*
import org.jetbrains.kotlin.konan.file.File as KonanFile

internal enum class KlibResolver { JS, Native }

internal fun KlibResolver.createModuleDescriptor(
    lib: File,
    inputDependencies: Set<File>,
    storageManager: StorageManager
): ModuleDescriptor {
    val factories = klibMetadataFactories()

    val library = resolveSingleFileKlib(KonanFile(lib.canonicalPath))

    val module = factories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
        library,
        LanguageVersionSettingsImpl.DEFAULT,
        storageManager,
        DefaultBuiltIns.Instance,
        null,
        LookupTracker.DO_NOTHING
    )

    val dependencies = libraryResolver(inputDependencies).resolveWithDependencies(
        unresolvedLibraries = library.unresolvedDependencies,
        noStdLib = this == KlibResolver.JS,
        noDefaultLibs = this == KlibResolver.JS,
        noEndorsedLibs = this == KlibResolver.JS
    )
    val dependenciesResolved = factories.DefaultResolvedDescriptorsFactory.createResolved(
        dependencies,
        storageManager,
        DefaultBuiltIns.Instance,
        LanguageVersionSettingsImpl.DEFAULT,
        emptySet(),
        emptySet(),
        emptySet(),
        emptyList(),
        isForMetadataCompilation = false
    )

    val dependenciesDescriptors = dependenciesResolved.resolvedDescriptors
    val forwardDeclarationsModule = dependenciesResolved.forwardDeclarationsModule

    module.setDependencies(listOf(module) + dependenciesDescriptors + forwardDeclarationsModule)
    return module
}

private fun KlibResolver.klibMetadataFactories() = KlibMetadataFactories(
    createBuiltIns = { DefaultBuiltIns.Instance },
    flexibleTypeDeserializer = when (this) {
        KlibResolver.JS -> DynamicTypeDeserializer
        KlibResolver.Native -> NullFlexibleTypeDeserializer
    }
)

private fun KlibResolver.libraryResolver(inputDependencies: Set<File>): KotlinLibraryResolverImpl<KotlinLibrary> {
    val logger = object : Logger {
        override fun log(message: String) {}
        override fun error(message: String) = kotlin.error("e: $message")
        override fun warning(message: String) {}
        override fun fatal(message: String) = kotlin.error("e: $message")
    }
    val deps = inputDependencies.map(File::getCanonicalPath)
    return KLibLibraryResolver(
        klibs = deps,
        knownIrProviders = when (this) {
            KlibResolver.Native -> listOf(KLIB_INTEROP_IR_PROVIDER_IDENTIFIER)
            KlibResolver.JS -> emptyList()
        },
        logger = logger
    ).libraryResolver()
}

private class KLibLibraryResolver(
    klibs: List<String>,
    knownIrProviders: List<String>,
    logger: Logger
) : KotlinLibraryProperResolverWithAttributes<KotlinLibrary>(
    repositories = emptyList(),
    directLibs = klibs,
    distributionKlib = null,
    localKotlinDir = null,
    skipCurrentDir = false,
    logger = logger,
    knownIrProviders = knownIrProviders
) {
    override fun libraryComponentBuilder(file: KonanFile, isDefault: Boolean): List<KotlinLibrary> =
        createKotlinLibraryComponents(file, isDefault)
}
