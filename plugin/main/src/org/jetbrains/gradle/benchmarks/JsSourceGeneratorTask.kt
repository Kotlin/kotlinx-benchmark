package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.workers.*
import org.jetbrains.kotlin.config.*
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
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var inputClassesDirs: FileCollection

    @OutputDirectory
    lateinit var outputResourcesDir: File

    @OutputDirectory
    lateinit var outputSourcesDir: File

    @TaskAction
    fun generate() {
        cleanup(outputSourcesDir)
        cleanup(outputResourcesDir)

        inputClassesDirs.files.forEach { lib ->
            val libs = KotlinJavascriptMetadataUtils.loadMetadata(lib)
            libs.forEach { metadata ->
                val configuration = CompilerConfiguration()

                val storageManager = LockBasedStorageManager()
                val languageVersionSettings = configuration.languageVersionSettings

                val skipCheck = true // languageVersionSettings.getFlag(AnalysisFlags.skipMetadataVersionCheck)
                assert(metadata.version.isCompatible() || skipCheck) {
                    "Expected JS metadata version " + JsMetadataVersion.INSTANCE + ", but actual metadata version is " + metadata.version
                }

                val module = ModuleDescriptorImpl(
                    Name.special("<" + metadata.moduleName + ">"),
                    storageManager,
                    JsPlatform.builtIns
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
                module.setDependencies(listOf(module, JsPlatform.builtIns.builtInsModule))
                module.initialize(provider)

                val generator = SuiteSourceGenerator(module, outputSourcesDir, Platform.JS)
                generator.generate()
            }
        }

    }
}


