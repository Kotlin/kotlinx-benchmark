package org.jetbrains.gradle.benchmarks

import com.squareup.kotlinpoet.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.workers.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.util.KonanFactories.DefaultDeserializedDescriptorFactory
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.storage.*
import java.io.*
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
        inputClassesDirs.files.filter { it.name.endsWith(KLIB_FILE_EXTENSION_WITH_DOT) }.forEach { lib ->
            val konanFile = org.jetbrains.kotlin.konan.file.File(lib.canonicalPath)

            val library = createKonanLibrary(konanFile, 1, PredefinedKonanTargets.getByName(target), false)
            println("Library: ${library.libraryName}, ${library.targetList}, ${library.unresolvedDependencies}")
            val storageManager = LockBasedStorageManager()
            val versionSpec = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE)
            val module = DefaultDeserializedDescriptorFactory.createDescriptorAndNewBuiltIns(
                library,
                versionSpec,
                storageManager
            )
            module.setDependencies(module)
            val benchmarks = mutableListOf<ClassName>()
            processPackage(module, module.getPackage(FqName.ROOT)) {
                benchmarks.generateBenchmark(it)
            }


/*
            val defaultModules = mutableListOf<ModuleDescriptorImpl>()
            if (!module.isKonanStdlib()) {
                val resolver = resolverByName(
                    emptyList(),
                    distributionKlib = Distribution().klib,
                    skipCurrentDir = true)
                
                resolver.defaultLinks(false, true)
                    .mapTo(defaultModules) {
                        DefaultDeserializedDescriptorFactory.createDescriptor(
                            it, versionSpec, storageManager, module.builtIns)
                    }
            }
            val allModules = defaultModules + module
            allModules.forEach { it.setDependencies(allModules) }
*/

        }
    }



    fun MutableList<ClassName>.generateBenchmark(original: ClassDescriptor) {
        val originalClass =
            ClassName(original.fqNameSafe.parent().toString(), original.fqNameSafe.shortName().toString())
        val packageName = original.fqNameSafe.parent().child(Name.identifier("generated")).toString()
        val benchmarkName = original.fqNameSafe.shortName().toString() + "_runner"
        val benchmarkClass = ClassName(packageName, benchmarkName)

        val benchmarks = DescriptorUtils.getAllDescriptors(original.unsubstitutedMemberScope)
            .filterIsInstance<FunctionDescriptor>()
            .filter { it.annotations.any { it.fqName.toString() == "org.jetbrains.gradle.benchmarks.Benchmark" } }

        val file = FileSpec.builder(packageName, benchmarkName).apply {
            declareClass(benchmarkClass) {
                property("_instance", originalClass) {
                    addModifiers(KModifier.PRIVATE)
                    initializer(codeBlock {
                        addStatement("%T()", originalClass)
                    })
                }

                for (benchmark in benchmarks) {
                    val functionName = benchmark.name.toString()
                    addFunction(
                        FunSpec.builder(functionName)
                            //                        .addStatement("println(%P)", "Benchmarking $functionName…")
//                            .beginControlFlow("repeat(1000)")
                            .addStatement("_instance.%N()", functionName)
                            //                          .endControlFlow()
                            .build()
                    )
                }

                addFunction(FunSpec.builder("addBenchmarkToSuite").apply {
                    addParameter("suite", Dynamic)
                    for (benchmark in benchmarks) {
                        val functionName = benchmark.name.toString()
                        addStatement("suite.add(%P) { %N() }", "${originalClass.canonicalName}.$functionName", functionName)
                    }
                }.build())

            }
            add(benchmarkClass)
        }.build()

        file.writeTo(outputSourcesDir)
    }
}

