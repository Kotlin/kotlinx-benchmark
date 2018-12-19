package org.jetbrains.gradle.benchmarks

import com.squareup.kotlinpoet.*
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
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.*
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

        inputClassesDirs.files.forEach {
            val libs = KotlinJavascriptMetadataUtils.loadMetadata(it)
            libs.forEach { metadata ->
                val configuration = CompilerConfiguration()

                val storageManager = LockBasedStorageManager()
                val languageVersionSettings = configuration.languageVersionSettings

                assert(metadata.version.isCompatible() || languageVersionSettings.getFlag(AnalysisFlag.skipMetadataVersionCheck)) {
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
                val benchmarks = mutableListOf<ClassName>()
                benchmarks.processPackage(module, module.getPackage(FqName.ROOT))

                val file = FileSpec.builder("", "BenchmarkSuite").apply {
                    addFunction(FunSpec.builder("require").apply {
                        addModifiers(KModifier.EXTERNAL)
                        addParameter("module", String::class)
                        returns(Dynamic)
                    }.build())
                    addFunction(FunSpec.builder("main").apply {
                        addStatement("val benchmarkjs = require(\"benchmark\")")
                        addStatement("val suite = benchmarkjs.Suite()")
                        for (benchmark in benchmarks) {
                            addStatement("%T().addBenchmarkToSuite(suite)", benchmark)
                        }
                        addStatement("suite.on(\"cycle\") { event-> println(event.target.toString()) }")
                        addStatement("println(%S)", "Running benchmarks…")
                        addStatement("suite.run()")
                        addStatement("println(%S)", "Complete!")
                    }.build())
                }.build()
                file.writeTo(outputSourcesDir)
            }
        }


/*
        workerExecutor.submit(JmhBytecodeGeneratorWorker::class.java) { config ->
            config.isolationMode = IsolationMode.PROCESS
            config.params(inputClassesDirs.files, outputSourcesDir, outputResourcesDir)
        }
*/
    }

    fun MutableList<ClassName>.processPackage(module: ModuleDescriptor, packageView: PackageViewDescriptor) {
        for (packageFragment in packageView.fragments.filter { it.module == module }) {
            DescriptorUtils.getAllDescriptors(packageFragment.getMemberScope())
                .filterIsInstance<ClassDescriptor>()
                .filter { it.annotations.any { it.fqName.toString() == "test.State" } }
                .forEach {
                    generateBenchmark(it)
                }
        }

        for (subpackageName in module.getSubPackagesOf(packageView.fqName, MemberScope.ALL_NAME_FILTER)) {
            processPackage(module, module.getPackage(subpackageName))
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
            .filter { it.annotations.any { it.fqName.toString() == "test.Benchmark" } }

        val file = FileSpec.builder(packageName, benchmarkName).apply {
            val clazz = declareClass(benchmarkClass) {
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
                        addStatement("suite.add(%P) { %N() }", "${originalClass.simpleName}.$functionName", functionName)
                    }
                }.build())

            }
            add(benchmarkClass)
        }.build()

        file.writeTo(outputSourcesDir)
    }
}


inline fun codeBlock(builderAction: CodeBlock.Builder.() -> Unit): CodeBlock {
    return CodeBlock.builder().apply(builderAction).build()
}

inline fun FileSpec.Builder.declareClass(name: String, builderAction: TypeSpec.Builder.() -> Unit): TypeSpec {
    return TypeSpec.classBuilder(name).apply(builderAction).build().also {
        addType(it)
    }
}

inline fun FileSpec.Builder.declareClass(name: ClassName, builderAction: TypeSpec.Builder.() -> Unit): TypeSpec {
    return TypeSpec.classBuilder(name).apply(builderAction).build().also {
        addType(it)
    }
}

inline fun TypeSpec.Builder.property(
    name: String,
    type: ClassName,
    builderAction: PropertySpec.Builder.() -> Unit
): PropertySpec {
    return PropertySpec.builder(name, type).apply(builderAction).build().also {
        addProperty(it)
    }
}
