package kotlinx.benchmark.gradle

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.workers.*
import org.jetbrains.kotlin.builtins.*
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

        inputClassesDirs.files.forEach { lib ->
            generateSources(lib)
        }
    }

    private fun load(lib: File): List<ModuleDescriptor> {
        val configuration = CompilerConfiguration()
        val languageVersionSettings = configuration.languageVersionSettings
        val storageManager = LockBasedStorageManager("Inspect")

        val dependencies = inputDependencies.flatMap {
            loadDescriptors(it, languageVersionSettings, storageManager)
        }
        return loadDescriptors(lib, languageVersionSettings, storageManager, dependencies)
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

    private fun loadDescriptors(
        lib: File,
        languageVersionSettings: LanguageVersionSettings,
        storageManager: LockBasedStorageManager,
        dependencies: List<ModuleDescriptorImpl> = listOf()
    ): List<ModuleDescriptorImpl> {
        val modules = KotlinJavascriptMetadataUtils.loadMetadata(lib)
        val builtIns = org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices.builtIns
        return modules.map { metadata ->
            val skipCheck = languageVersionSettings.getFlag(AnalysisFlags.skipMetadataVersionCheck)
            assert(metadata.version.isCompatible() || skipCheck) {
                "Expected JS metadata version " + JsMetadataVersion.INSTANCE + ", but actual metadata version is " + metadata.version
            }

            val module = ModuleDescriptorImpl(
                Name.special("<" + metadata.moduleName + ">"),
                storageManager,
                builtIns
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
                CompilerDeserializationConfiguration(languageVersionSettings),
                LookupTracker.DO_NOTHING
            )
            module.setDependencies(listOf(module, builtIns.builtInsModule) + dependencies)
            module.initialize(provider)
            module
        }
    }

}


