package kotlinx.benchmark.gradle

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.workers.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.js.resolve.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.serialization.js.*
import org.jetbrains.kotlin.storage.*
import org.jetbrains.kotlin.utils.*
import java.io.*
import javax.inject.*

@Suppress("UnstableApiUsage")
@CacheableTask
open class JsSourceGeneratorTask
@Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {
    @Input
    lateinit var title: String

    @Input
    var ir: Boolean = false

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

    @TaskAction
    fun generate() {
        cleanup(outputSourcesDir)
        cleanup(outputResourcesDir)

        inputClassesDirs.files.forEach { lib: File ->
            generateSources(lib)
        }
    }

    private fun generateSources(lib: File) {
        val modules = load(lib)
        modules.forEach { module ->
            val generator = SuiteSourceGenerator(
                title,
                module,
                outputSourcesDir,
                Platform.JS
            )
            generator.generate()
        }
    }

    private fun load(lib: File): List<ModuleDescriptor> {
        val storageManager = LockBasedStorageManager("Inspect")
        return if (ir) {
            loadIr(lib, storageManager)
        } else {
            loadLegacy(lib, storageManager)
        }
    }

    private fun loadIr(lib: File, storageManager: StorageManager): List<ModuleDescriptor> {
        //skip processing of empty dirs (fail if not to do it)
        if (lib.listFiles() == null) return emptyList()
        val dependencies = inputDependencies.files.filterNot { it.extension == "js" }.toSet()
        val module = KlibResolver.JS.createModuleDescriptor(lib, dependencies, storageManager)
        return listOf(module)
    }

    private fun loadLegacy(lib: File, storageManager: StorageManager): List<ModuleDescriptor> {
        val dependencies = inputDependencies.flatMap {
            loadDescriptors(it, storageManager)
        }
        return loadDescriptors(lib, storageManager, dependencies)
    }

    private fun loadDescriptors(
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

}


