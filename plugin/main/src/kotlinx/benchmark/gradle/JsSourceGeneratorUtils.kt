package kotlinx.benchmark.gradle

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.serialization.js.createKotlinJavascriptPackageFragmentProvider
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.JsMetadataVersion
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.io.File

internal fun loadJsDescriptors(
    lib: File,
    storageManager: StorageManager,
    dependencies: List<ModuleDescriptorImpl> = listOf()
): List<ModuleDescriptorImpl> = KotlinJavascriptMetadataUtils.loadMetadata(lib).map { metadata ->
    val skipCheck = LanguageVersionSettingsImpl.DEFAULT.getFlag(AnalysisFlags.skipMetadataVersionCheck)
    assert(metadata.version.isCompatible() || skipCheck) {
        "Expected JS metadata version " + JsMetadataVersion.INSTANCE + ", but actual metadata version is " + metadata.version
    }

    val module = ModuleDescriptorImpl(
        Name.special("<" + metadata.moduleName + ">"),
        storageManager,
        JsPlatformAnalyzerServices.builtIns
    )
    val (header, body) = KotlinJavascriptSerializationUtil.readModuleAsProto(
        metadata.body,
        metadata.version
    )
    val provider = createKotlinJavascriptPackageFragmentProvider(
        storageManager,
        module,
        header,
        body,
        metadata.version,
        CompilerDeserializationConfiguration(LanguageVersionSettingsImpl.DEFAULT),
        LookupTracker.DO_NOTHING
    )
    module.setDependencies(listOf(module, JsPlatformAnalyzerServices.builtIns.builtInsModule) + dependencies)
    module.initialize(provider)
    module
}