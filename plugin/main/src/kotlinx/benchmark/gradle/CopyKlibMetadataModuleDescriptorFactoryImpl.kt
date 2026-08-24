/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.benchmark.gradle

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.functionInterfacePackageFragmentProvider
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.ContractDeserializerImpl
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isAnyPlatformStdlib
import org.jetbrains.kotlin.library.metadata.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.resolve.ImplicitIntegerCoercion
import org.jetbrains.kotlin.resolve.KlibCompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.sam.SamConversionResolverImpl
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.addToStdlib.runIf

internal class CopyKlibMetadataModuleDescriptorFactoryImpl(
    override val descriptorFactory: KlibModuleDescriptorFactory,
    override val packageFragmentsFactory: KlibMetadataDeserializedPackageFragmentsFactory,
    override val flexibleTypeDeserializer: FlexibleTypeDeserializer
) : KlibMetadataModuleDescriptorFactory {
    companion object {
        internal fun KlibModuleOrigin.isCInteropLibrary(): Boolean = when (this) {
            is DeserializedKlibModuleOrigin -> this.library.isCInteropLibrary()
            CurrentKlibModuleOrigin, SyntheticModulesOrigin -> false
        }
    }

    override fun createDescriptorOptionalBuiltIns(
        library: KotlinLibrary,
        languageVersionSettings: LanguageVersionSettings,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns?,
        packageAccessHandler: PackageAccessHandler?,
        lookupTracker: LookupTracker
    ): ModuleDescriptorImpl {

        val libraryProto = parseModuleHeader(library.moduleHeaderData)

        val moduleName = Name.special(libraryProto.moduleName)
        val moduleOrigin = DeserializedKlibModuleOrigin(library)

        val builtInsToUse = builtIns ?: DefaultBuiltIns.Instance
        val moduleDescriptor = ModuleDescriptorImpl(
            moduleName,
            storageManager,
            builtInsToUse,
            capabilities = mapOf(
                KlibModuleOrigin.CAPABILITY to moduleOrigin,
                ImplicitIntegerCoercion.MODULE_CAPABILITY to moduleOrigin.isCInteropLibrary()
            ),
            platform = NativePlatforms.unspecifiedNativePlatform
        )

        if (builtIns == null) {
            builtInsToUse.builtInsModule = moduleDescriptor
        }

        val provider = createPackageFragmentProvider(
            library,
            packageAccessHandler,
            packageFragmentNames = libraryProto.packageFragmentNameList,
            storageManager,
            moduleDescriptor,
            configuration = KlibCompilerDeserializationConfiguration(languageVersionSettings),
            compositePackageFragmentAddend = runIf(library.isAnyPlatformStdlib) {
                functionInterfacePackageFragmentProvider(storageManager, moduleDescriptor)
            },
            lookupTracker = lookupTracker
        )

        moduleDescriptor.initialize(provider)

        return moduleDescriptor
    }

    override fun createCachedPackageFragmentProvider(
        byteArrays: List<ByteArray>,
        storageManager: StorageManager,
        moduleDescriptor: ModuleDescriptor,
        configuration: DeserializationConfiguration,
        lookupTracker: LookupTracker
    ): PackageFragmentProvider {
        val deserializedPackageFragments = packageFragmentsFactory.createCachedPackageFragments(
            byteArrays, moduleDescriptor, storageManager
        )

        val provider = PackageFragmentProviderImpl(deserializedPackageFragments)
        return initializePackageFragmentProvider(provider, deserializedPackageFragments, storageManager,
            moduleDescriptor, configuration, null, lookupTracker)
    }

    override fun createPackageFragmentProvider(
        library: KotlinLibrary,
        packageAccessHandler: PackageAccessHandler?,
        packageFragmentNames: List<String>,
        storageManager: StorageManager,
        moduleDescriptor: ModuleDescriptor,
        configuration: DeserializationConfiguration,
        compositePackageFragmentAddend: PackageFragmentProvider?,
        lookupTracker: LookupTracker
    ): PackageFragmentProvider {

        val deserializedPackageFragments = packageFragmentsFactory.createDeserializedPackageFragments(
            library, packageFragmentNames, moduleDescriptor, packageAccessHandler, storageManager, configuration
        )

        // Generate empty PackageFragmentDescriptor instances for packages that aren't mentioned in compilation units directly.
        // For example, if there's `package foo.bar` directive, we'll get only PackageFragmentDescriptor for `foo.bar`, but
        // none for `foo`. Various descriptor/scope code relies on presence of such package fragments, and currently we
        // don't know if it's possible to fix this.
        // TODO: think about fixing issues in descriptors/scopes
        val packageFqNames = deserializedPackageFragments.mapTo(mutableSetOf()) { it.fqName }
        val emptyPackageFragments = mutableListOf<PackageFragmentDescriptor>()
        for (packageFqName in packageFqNames.mapNotNull { it.parentOrNull() }) {
            var ancestorFqName = packageFqName
            while (!ancestorFqName.isRoot && packageFqNames.add(ancestorFqName)) {
                emptyPackageFragments += EmptyPackageFragmentDescriptor(moduleDescriptor, ancestorFqName)
                ancestorFqName = ancestorFqName.parent()
            }
        }

        val provider = PackageFragmentProviderImpl(deserializedPackageFragments + emptyPackageFragments)
        return initializePackageFragmentProvider(provider, deserializedPackageFragments, storageManager,
            moduleDescriptor, configuration, compositePackageFragmentAddend, lookupTracker)
    }

    fun initializePackageFragmentProvider(
        provider: PackageFragmentProviderImpl,
        fragmentsToInitialize: List<DeserializedPackageFragment>,
        storageManager: StorageManager,
        moduleDescriptor: ModuleDescriptor,
        configuration: DeserializationConfiguration,
        compositePackageFragmentAddend: PackageFragmentProvider?,
        lookupTracker: LookupTracker
    ): PackageFragmentProvider {

        val notFoundClasses = NotFoundClasses(storageManager, moduleDescriptor)

        val annotationAndConstantLoader = AnnotationAndConstantLoaderImpl(
            moduleDescriptor,
            notFoundClasses,
            KlibMetadataSerializerProtocol
        )

        val enumEntriesDeserializationSupport = object : EnumEntriesDeserializationSupport {
            override fun canSynthesizeEnumEntries(): Boolean = moduleDescriptor.platform.isJvm()
        }

        val components = DeserializationComponents(
            storageManager,
            moduleDescriptor,
            configuration,
            DeserializedClassDataFinder(provider),
            annotationAndConstantLoader,
            provider,
            LocalClassifierTypeSettings.Default,
            ErrorReporter.DO_NOTHING,
            lookupTracker,
            flexibleTypeDeserializer,
            emptyList(),
            notFoundClasses,
            ContractDeserializerImpl(configuration, storageManager),
            extensionRegistryLite = KlibMetadataSerializerProtocol.extensionRegistry,
            samConversionResolver = SamConversionResolverImpl(storageManager, samWithReceiverResolvers = emptyList()),
            enumEntriesDeserializationSupport = enumEntriesDeserializationSupport,
        )

        fragmentsToInitialize.forEach {
            it.initialize(components)
        }

        return compositePackageFragmentAddend?.let {
            CompositePackageFragmentProvider(
                listOf(it, provider),
                "CompositeProvider@KlibMetadataModuleDescriptorFactory for $moduleDescriptor"
            )
        } ?: provider
    }
}
