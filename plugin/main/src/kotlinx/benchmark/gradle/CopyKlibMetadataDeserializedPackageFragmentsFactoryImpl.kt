package kotlinx.benchmark.gradle

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.BuiltInKlibMetadataDeserializedPackageFragment
import org.jetbrains.kotlin.library.metadata.KlibDeserializedContainerSource
import org.jetbrains.kotlin.library.metadata.KlibMetadataCachedPackageFragment
import org.jetbrains.kotlin.library.metadata.KlibMetadataDeserializedPackageFragment
import org.jetbrains.kotlin.library.metadata.KlibMetadataDeserializedPackageFragmentsFactory
import org.jetbrains.kotlin.library.metadata.PackageAccessHandler
import org.jetbrains.kotlin.library.metadata.SimplePackageAccessHandler
import org.jetbrains.kotlin.library.metadata.getIncompatibility
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.storage.StorageManager

internal class CopyKlibMetadataDeserializedPackageFragmentsFactoryImpl : KlibMetadataDeserializedPackageFragmentsFactory {
    override fun createDeserializedPackageFragments(
        library: KotlinLibrary,
        packageFragmentNames: List<String>,
        moduleDescriptor: ModuleDescriptor,
        packageAccessedHandler: PackageAccessHandler?,
        storageManager: StorageManager,
        configuration: DeserializationConfiguration
    ): List<KlibMetadataDeserializedPackageFragment> {
        val libraryHeader = (packageAccessedHandler ?: SimplePackageAccessHandler).loadModuleHeader(library)

        return packageFragmentNames.flatMap {
            val packageFqName = FqName(it)
            val containerSource = KlibDeserializedContainerSource(
                library, libraryHeader, configuration, packageFqName, incompatibility = library.getIncompatibility(configuration.metadataVersion)
            )
            val parts = library.packageMetadataParts(packageFqName.asString())
            val isBuiltInModule = moduleDescriptor.builtIns.builtInsModule === moduleDescriptor
            parts.map { partName ->
                if (isBuiltInModule)
                    BuiltInKlibMetadataDeserializedPackageFragment(
                        packageFqName,
                        library,
                        packageAccessedHandler,
                        storageManager,
                        moduleDescriptor,
                        partName,
                        containerSource
                    )
                else
                    KlibMetadataDeserializedPackageFragment(
                        packageFqName,
                        library,
                        packageAccessedHandler,
                        storageManager,
                        moduleDescriptor,
                        partName,
                        containerSource
                    )
            }
        }
    }

    override fun createCachedPackageFragments(
        packageFragments: List<ByteArray>,
        moduleDescriptor: ModuleDescriptor,
        storageManager: StorageManager
    ) = packageFragments.map { byteArray ->
        KlibMetadataCachedPackageFragment(byteArray, storageManager, moduleDescriptor)
    }

}
