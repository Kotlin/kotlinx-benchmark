package kotlinx.benchmark.gradle

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import kotlinx.benchmark.gradle.internal.generator.RequiresKotlinCompilerEmbeddable
import kotlinx.metadata.klib.KlibModuleMetadata
import kotlinx.metadata.klib.annotations
import kotlinx.metadata.klib.fqName
import kotlinx.metadata.klib.uniqId
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import java.io.File
import kotlin.metadata.KmAnnotationArgument
import kotlin.metadata.KmClass
import kotlin.metadata.KmClassifier
import kotlin.metadata.modality

@KotlinxBenchmarkPluginInternalApi
enum class Platform(
    val executorClass: String,
    val suiteDescriptorClass: String,
    val benchmarkDescriptorClass: String,
    val benchmarkDescriptorWithBlackholeParameterClass: String
) {
    JsBuiltIn(
        executorClass = "kotlinx.benchmark.js.JsBuiltInExecutor",
        suiteDescriptorClass = "kotlinx.benchmark.SuiteDescriptor",
        benchmarkDescriptorClass = "kotlinx.benchmark.js.JsBenchmarkDescriptorWithNoBlackholeParameter",
        benchmarkDescriptorWithBlackholeParameterClass = "kotlinx.benchmark.js.JsBenchmarkDescriptorWithBlackholeParameter",
    ),
    JsBenchmarkJs(
        executorClass = "kotlinx.benchmark.js.JsBenchmarkExecutor",
        suiteDescriptorClass = "kotlinx.benchmark.SuiteDescriptor",
        benchmarkDescriptorClass = "kotlinx.benchmark.js.JsBenchmarkDescriptorWithNoBlackholeParameter",
        benchmarkDescriptorWithBlackholeParameterClass = "kotlinx.benchmark.js.JsBenchmarkDescriptorWithBlackholeParameter",
    ),
    NativeBuiltIn(
        executorClass = "kotlinx.benchmark.native.NativeExecutor",
        suiteDescriptorClass = "kotlinx.benchmark.SuiteDescriptor",
        benchmarkDescriptorClass = "kotlinx.benchmark.BenchmarkDescriptorWithNoBlackholeParameter",
        benchmarkDescriptorWithBlackholeParameterClass = "kotlinx.benchmark.BenchmarkDescriptorWithBlackholeParameter",
    ),
    WasmBuiltIn(
        executorClass = "kotlinx.benchmark.wasm.WasmBuiltInExecutor",
        suiteDescriptorClass = "kotlinx.benchmark.SuiteDescriptor",
        benchmarkDescriptorClass = "kotlinx.benchmark.BenchmarkDescriptorWithNoBlackholeParameter",
        benchmarkDescriptorWithBlackholeParameterClass = "kotlinx.benchmark.BenchmarkDescriptorWithBlackholeParameter",
    )
}

@KotlinxBenchmarkPluginInternalApi
@RequiresKotlinCompilerEmbeddable
class SuiteSourceGenerator(
    val title: String,
    val metadata: KlibModuleMetadata,
    val output: File,
    val platform: Platform
) {

    @KotlinxBenchmarkPluginInternalApi
    companion object {
        val setupFunctionName = "setUp"
        val teardownFunctionName = "tearDown"
        val parametersFunctionName = "parametrize"

        val externalConfigurationFQN = "kotlinx.benchmark.ExternalConfiguration"
        val benchmarkAnnotationFQN = "kotlinx.benchmark.Benchmark"
        val benchmarkAnnotationClassName = "kotlinx/benchmark/Benchmark"
        val setupAnnotationFQN = "kotlinx.benchmark.Setup"
        val teardownAnnotationFQN = "kotlinx.benchmark.TearDown"
        val setupAnnotationClassName = "kotlinx/benchmark/Setup"
        val teardownAnnotationClassName = "kotlinx/benchmark/TearDown"
        val stateAnnotationFQN = "kotlinx.benchmark.State"
        val stateAnnotationClassName = "kotlinx/benchmark/State"
        val modeAnnotationFQN = "kotlinx.benchmark.BenchmarkMode"
        val modeAnnotationClassName = "kotlinx/benchmark/BenchmarkMode"
        val timeUnitFQN = "kotlinx.benchmark.BenchmarkTimeUnit"
        val timeUnitClassName = "kotlinx/benchmark/BenchmarkTimeUnit"
        val iterationTimeFQN = "kotlinx.benchmark.IterationTime"
        val iterationTimeClassName = "kotlinx/benchmark/IterationTime"
        val modeFQN = "kotlinx.benchmark.Mode"
        val modeClassName = "kotlinx/benchmark/Mode"
        val outputTimeAnnotationFQN = "kotlinx.benchmark.OutputTimeUnit"
        val outputTimeAnnotationClassName = "kotlinx/benchmark/OutputTimeUnit"
        val warmupAnnotationFQN = "kotlinx.benchmark.Warmup"
        val warmupAnnotationClassName = "kotlinx/benchmark/Warmup"
        val measureAnnotationFQN = "kotlinx.benchmark.Measurement"
        val measureAnnotationClassName = "kotlinx/benchmark/Measurement"
        val paramAnnotationFQN = "kotlinx.benchmark.Param"
        val paramAnnotationClassName = "kotlinx/benchmark/Param"


        val blackholeFQN = "kotlinx.benchmark.Blackhole"
        val blackholeClassName = "kotlinx/benchmark/Blackhole"


        val mainBenchmarkPackage = "kotlinx.benchmark.generated"

        val suppressWarnings = AnnotationSpec.builder(Suppress::class).addMember(
            "\"UNUSED_PARAMETER\", \"REDUNDANT_CALL_OF_CONVERSION_METHOD\""
        ).build()
        val optInRuntimeInternalApi = AnnotationSpec.builder(ClassName("kotlin", "OptIn")).addMember(
            "kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi::class"
        ).build()
    }

    private val executorType = ClassName.bestGuess(platform.executorClass)
    private val suiteDescriptorType = ClassName.bestGuess(platform.suiteDescriptorClass)

    val benchmarks = mutableListOf<ClassName>()

    fun generate() {
        processPackage(metadata)
        generateRunnerMain()
    }

    private fun generateRunnerMain() {
        val file = FileSpec.builder(mainBenchmarkPackage, "BenchmarkSuite").apply {
            function("main") {
                addAnnotation(optInRuntimeInternalApi)
                val array = ClassName("kotlin", "Array")
                val arrayOfStrings = array.parameterizedBy(WildcardTypeName.producerOf(String::class))
                addParameter("args", arrayOfStrings)
                addStatement("val executor = %T(%S, args)", executorType, title)
                for (benchmark in benchmarks) {
                    addStatement("executor.suite(%T.describe())", benchmark)
                }
                addStatement("executor.run()")
            }
        }.build()
        file.writeTo(output)
    }

    private fun processPackage(metadata: KlibModuleMetadata) {
        metadata.fragments.forEach { moduleFragment ->
            moduleFragment.classes.forEach { klass ->
                if (klass.annotations.none { it.className == stateAnnotationClassName }) return@forEach
                if (klass.modality == kotlin.metadata.Modality.ABSTRACT) return@forEach
                generateBenchmark(klass)
            }
        }
    }

    private fun generateBenchmark(original: KmClass) {
        val originalPackage = if (original.name.contains('/')) {
            original.name.substringBeforeLast('/').replace('/', '.')
        } else {
            ""
        }
        val originalName = original.name.substringAfterLast('/')
        val originalClass = ClassName(originalPackage, originalName)

        val benchmarkPackageName = mainBenchmarkPackage + if (originalPackage.isNotEmpty()) ".$originalPackage" else ""
        val benchmarkName = "${originalName}_Descriptor"
        val benchmarkClass = ClassName(benchmarkPackageName, benchmarkName)

        val functions = original.functions

        val parameterProperties = original.properties.filter {
            it.annotations.any { it.className == paramAnnotationClassName }
        }

        validateParameterProperties(parameterProperties)

        val measureAnnotation = original.annotations.singleOrNull { it.className == measureAnnotationClassName }
        val warmupAnnotation = original.annotations.singleOrNull { it.className == warmupAnnotationClassName }
        val outputTimeAnnotation = original.annotations.singleOrNull { it.className == outputTimeAnnotationClassName }
        val modeAnnotation = original.annotations.singleOrNull { it.className == modeAnnotationClassName }

        val outputTimeUnitValue = outputTimeAnnotation?.arguments?.get("value") as KmAnnotationArgument.EnumValue?
        val outputTimeUnit = outputTimeUnitValue?.enumEntryName

        @Suppress("UNCHECKED_CAST")
        val modesValue = modeAnnotation?.arguments?.get("value") as KmAnnotationArgument.ArrayValue?
        val mode = modesValue?.elements?.map { it as KmAnnotationArgument.EnumValue }?.single()?.enumEntryName

        val measureIterations = (measureAnnotation?.arguments?.get("iterations") as KmAnnotationArgument.IntValue?)?.value
        val measureIterationTime = (measureAnnotation?.arguments?.get("time") as KmAnnotationArgument.IntValue?)?.value
        val measureIterationTimeUnit = measureAnnotation?.arguments?.get("timeUnit") as KmAnnotationArgument.EnumValue?

        val warmupIterations = (warmupAnnotation?.arguments?.get("iterations") as KmAnnotationArgument.IntValue?)?.value

        val iterations = measureIterations
        val iterationTime = measureIterationTime
        val iterationTimeUnit = measureIterationTimeUnit?.enumEntryName ?: "SECONDS"
        val warmups = warmupIterations

        val benchmarkFunctions =
            functions.filter { it.annotations.any { it.className == benchmarkAnnotationClassName } }

        validateBenchmarkFunctions(benchmarkFunctions)

        val setupFunctions = functions.filter { it.annotations.any { it.className == setupAnnotationClassName } }

        validateSetupFunctions(setupFunctions)

        val teardownFunctions = functions
            .filter { it.annotations.any { it.className == teardownAnnotationClassName } }.reversed()

        validateTeardownFunctions(teardownFunctions)

        val file = FileSpec.builder(benchmarkPackageName, benchmarkName).apply {
            declareObject(benchmarkClass) {
                addAnnotation(suppressWarnings)
                addAnnotation(optInRuntimeInternalApi)

                function(setupFunctionName) {
                    addModifiers(KModifier.PRIVATE)
                    addParameter("instance", originalClass)
                    for (fn in setupFunctions) {
                        val functionName = fn.name
                        addStatement("instance.%N()", functionName)
                    }
                }

                function(teardownFunctionName) {
                    addModifiers(KModifier.PRIVATE)
                    addParameter("instance", originalClass)
                    for (fn in teardownFunctions) {
                        val functionName = fn.name
                        addStatement("instance.%N()", functionName)
                    }
                }

                function(parametersFunctionName) {
                    addModifiers(KModifier.PRIVATE)
                    addParameter("instance", originalClass)
                    addParameter("params", MAP.parameterizedBy(STRING, STRING))

                    parameterProperties.forEach { property ->
                        val type = (property.returnType.classifier as KmClassifier.Class).name.substringAfterLast('/')
                        addStatement("instance.${property.name} = params.getValue(\"${property.name}\").to$type()")
                    }
                }

                val defaultParameters = parameterProperties.associateBy({ it.name }, {
                    val annotation = it.annotations.find { it.className == paramAnnotationClassName }!!
                    @Suppress("UNCHECKED_CAST")
                    val value = annotation.arguments["value"]!! as KmAnnotationArgument.ArrayValue
                    value.elements.map { (it as KmAnnotationArgument.StringValue).value }
                })

                val defaultParametersString = defaultParameters.entries
                    .joinToString(prefix = "mapOf(", postfix = ")") { (key, value) ->
                        val joinedValues = value.joinToString {
                            "\"\"\"${it.replace(' ', '·')}\"\"\""
                        }
                        "\"${key}\" to listOf($joinedValues)"
                    }

                val timeUnitClass = ClassName.bestGuess(timeUnitFQN)
                val iterationTimeClass = ClassName.bestGuess(iterationTimeFQN)
                val modeClass = ClassName.bestGuess(modeFQN)

                function("describe") {
                    returns(suiteDescriptorType.parameterizedBy(originalClass))
                    addCode(
                        "«val descriptor = %T(name = %S, factory = ::%T, setup = ::%N, teardown = ::%N, parametrize = ::%N",
                        suiteDescriptorType,
                        originalName,
                        originalClass,
                        setupFunctionName,
                        teardownFunctionName,
                        parametersFunctionName
                    )

                    val params =
                        parameterProperties.joinToString(prefix = "listOf(", postfix = ")") { "\"${it.name}\"" }
                    addCode(", parameters = $params")

                    addCode(", defaultParameters = $defaultParametersString")

                    if (iterations != null)
                        addCode(", iterations = $iterations")
                    if (warmups != null)
                        addCode(", warmups = $warmups")
                    if (iterationTime != null)
                        addCode(
                            ", iterationTime = %T($measureIterationTime, %T.%N)",
                            iterationTimeClass,
                            timeUnitClass,
                            MemberName(timeUnitClass, iterationTimeUnit)
                        )
                    if (outputTimeUnit != null)
                        addCode(
                            ", outputTimeUnit = %T.%N", timeUnitClass,
                            MemberName(timeUnitClass, outputTimeUnit)
                        )
                    if (mode != null)
                        addCode(
                            ", mode = %T.%N", modeClass,
                            MemberName(modeClass, mode)
                        )
                    addCode(")\n»")
                    addStatement("")

                    val bhClass = ClassName.bestGuess(blackholeFQN)
                    for (fn in benchmarkFunctions) {
                        val functionName = fn.name

                        val hasABlackholeParameter =
                            (fn.valueParameters.singleOrNull()?.type?.classifier as? KmClassifier.Class)?.name == blackholeClassName

                        val fqnDescriptorToCreate =
                            if (hasABlackholeParameter) platform.benchmarkDescriptorWithBlackholeParameterClass
                            else platform.benchmarkDescriptorClass

                        addStatement(
                            "descriptor.add(%T(%S, descriptor, %T(), %T::%N))",
                            ClassName.bestGuess(fqnDescriptorToCreate),
                            "${originalClass.canonicalName}.$functionName",
                            bhClass,
                            originalClass,
                            functionName
                        )
                    }
                    addStatement("return descriptor")
                }

            }
            benchmarks.add(benchmarkClass)
        }.build()

        file.writeTo(output)
    }
}

@KotlinxBenchmarkPluginInternalApi
inline fun codeBlock(builderAction: CodeBlock.Builder.() -> Unit): CodeBlock {
    return CodeBlock.builder().apply(builderAction).build()
}

@KotlinxBenchmarkPluginInternalApi
inline fun FileSpec.Builder.declareObject(name: ClassName, builderAction: TypeSpec.Builder.() -> Unit): TypeSpec {
    return TypeSpec.objectBuilder(name).apply(builderAction).build().also {
        addType(it)
    }
}

@KotlinxBenchmarkPluginInternalApi
inline fun FileSpec.Builder.declareClass(name: String, builderAction: TypeSpec.Builder.() -> Unit): TypeSpec {
    return TypeSpec.classBuilder(name).apply(builderAction).build().also {
        addType(it)
    }
}

@KotlinxBenchmarkPluginInternalApi
inline fun FileSpec.Builder.declareClass(name: ClassName, builderAction: TypeSpec.Builder.() -> Unit): TypeSpec {
    return TypeSpec.classBuilder(name).apply(builderAction).build().also {
        addType(it)
    }
}

@KotlinxBenchmarkPluginInternalApi
inline fun TypeSpec.Builder.property(
    name: String,
    type: ClassName,
    builderAction: PropertySpec.Builder.() -> Unit
): PropertySpec {
    return PropertySpec.builder(name, type).apply(builderAction).build().also {
        addProperty(it)
    }
}

@KotlinxBenchmarkPluginInternalApi
inline fun TypeSpec.Builder.function(
    name: String,
    builderAction: FunSpec.Builder.() -> Unit
): FunSpec {
    return FunSpec.builder(name).apply(builderAction).build().also {
        addFunction(it)
    }
}

@KotlinxBenchmarkPluginInternalApi
inline fun FileSpec.Builder.function(
    name: String,
    builderAction: FunSpec.Builder.() -> Unit
): FunSpec {
    return FunSpec.builder(name).apply(builderAction).build().also {
        addFunction(it)
    }
}

@KotlinxBenchmarkPluginInternalApi
@RequiresKotlinCompilerEmbeddable
val KotlinType.nameIfStandardType: Name?
    get() = constructor.declarationDescriptor?.name
