package kotlinx.benchmark.gradle

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.annotations.*
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.types.*
import java.io.*

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
class SuiteSourceGenerator(val title: String, val module: ModuleDescriptor, val output: File, val platform: Platform) {

    @KotlinxBenchmarkPluginInternalApi
    companion object {
        val setupFunctionName = "setUp"
        val teardownFunctionName = "tearDown"
        val parametersFunctionName = "parametrize"

        val externalConfigurationFQN = "kotlinx.benchmark.ExternalConfiguration"
        val benchmarkAnnotationFQN = "kotlinx.benchmark.Benchmark"
        val setupAnnotationFQN = "kotlinx.benchmark.Setup"
        val teardownAnnotationFQN = "kotlinx.benchmark.TearDown"
        val stateAnnotationFQN = "kotlinx.benchmark.State"
        val modeAnnotationFQN = "kotlinx.benchmark.BenchmarkMode"
        val timeUnitFQN = "kotlinx.benchmark.BenchmarkTimeUnit"
        val iterationTimeFQN = "kotlinx.benchmark.IterationTime"
        val modeFQN = "kotlinx.benchmark.Mode"
        val outputTimeAnnotationFQN = "kotlinx.benchmark.OutputTimeUnit"
        val warmupAnnotationFQN = "kotlinx.benchmark.Warmup"
        val measureAnnotationFQN = "kotlinx.benchmark.Measurement"
        val paramAnnotationFQN = "kotlinx.benchmark.Param"

        val blackholeFQN = "kotlinx.benchmark.Blackhole"

        val mainBenchmarkPackage = "kotlinx.benchmark.generated"

        val suppressUnusedParameter = AnnotationSpec.builder(Suppress::class).addMember("\"UNUSED_PARAMETER\"").build()
        val optInRuntimeInternalApi = AnnotationSpec.builder(ClassName("kotlin", "OptIn")).addMember(
            "kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi::class"
        ).build()
    }

    private val executorType = ClassName.bestGuess(platform.executorClass)
    private val suiteDescriptorType = ClassName.bestGuess(platform.suiteDescriptorClass)

    val benchmarks = mutableListOf<ClassName>()

    fun generate() {
        processPackage(module, module.getPackage(FqName.ROOT))
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

    private fun processPackage(module: ModuleDescriptor, packageView: PackageViewDescriptor) {
        for (packageFragment in packageView.fragments.filter { it.module == module }) {
            DescriptorUtils.getAllDescriptors(packageFragment.getMemberScope())
                .filterIsInstance<ClassDescriptor>()
                .filter { it.annotations.any { it.fqName.toString() == stateAnnotationFQN } }
                .filter { it.modality != Modality.ABSTRACT }
                .forEach {
                    generateBenchmark(it)
                }
        }

        for (subpackageName in module.getSubPackagesOf(packageView.fqName, MemberScope.ALL_NAME_FILTER)) {
            processPackage(module, module.getPackage(subpackageName))
        }
    }

    private fun generateBenchmark(original: ClassDescriptor) {
        val originalFqName = original.fqNameSafe
        val originalPackage = originalFqName.parent().let {
            if (it.isRoot) "" else it.asString()
        }
        val originalName = originalFqName.shortName().toString()
        val originalClass = ClassName(originalPackage, originalName)

        val benchmarkPackageName = mainBenchmarkPackage + if (originalPackage.isNotEmpty()) ".$originalPackage" else ""
        val benchmarkName = "${originalName}_Descriptor"
        val benchmarkClass = ClassName(benchmarkPackageName, benchmarkName)

        val functions = DescriptorUtils.getAllDescriptors(original.unsubstitutedMemberScope)
            .filterIsInstance<FunctionDescriptor>()

        val parameterProperties = DescriptorUtils.getAllDescriptors(original.unsubstitutedMemberScope)
            .filterIsInstance<PropertyDescriptor>()
            .filter { it.annotations.any { it.fqName.toString() == paramAnnotationFQN } }

        validateParameterProperties(parameterProperties)

        val measureAnnotation = original.annotations.singleOrNull { it.fqName.toString() == measureAnnotationFQN }
        val warmupAnnotation = original.annotations.singleOrNull { it.fqName.toString() == warmupAnnotationFQN }
        val outputTimeAnnotation = original.annotations.singleOrNull { it.fqName.toString() == outputTimeAnnotationFQN }
        val modeAnnotation = original.annotations.singleOrNull { it.fqName.toString() == modeAnnotationFQN }

        val outputTimeUnitValue = outputTimeAnnotation?.argumentValue("value") as EnumValue?
        val outputTimeUnit = outputTimeUnitValue?.enumEntryName?.toString()

        @Suppress("UNCHECKED_CAST")
        val modesValue = modeAnnotation?.argumentValue("value")?.value as List<EnumValue>?
        val mode = modesValue?.single()?.enumEntryName?.toString()

        val measureIterations = measureAnnotation?.argumentValue("iterations")?.value as Int?
        val measureIterationTime = measureAnnotation?.argumentValue("time")?.value as Int?
        val measureIterationTimeUnit = measureAnnotation?.argumentValue("timeUnit") as EnumValue?

        val warmupIterations = warmupAnnotation?.argumentValue("iterations")?.value as Int?

        val iterations = measureIterations
        val iterationTime = measureIterationTime
        val iterationTimeUnit = measureIterationTimeUnit?.enumEntryName?.toString() ?: "SECONDS"
        val warmups = warmupIterations

        val benchmarkFunctions =
            functions.filter { it.annotations.any { it.fqName.toString() == benchmarkAnnotationFQN } }

        validateBenchmarkFunctions(benchmarkFunctions)

        val setupFunctions = functions
            .filter { it.annotations.any { it.fqName.toString() == setupAnnotationFQN } }

        validateSetupFunctions(setupFunctions)

        val teardownFunctions = functions
            .filter { it.annotations.any { it.fqName.toString() == teardownAnnotationFQN } }.reversed()

        validateTeardownFunctions(teardownFunctions)

        val file = FileSpec.builder(benchmarkPackageName, benchmarkName).apply {
            declareObject(benchmarkClass) {
                addAnnotation(suppressUnusedParameter)
                addAnnotation(optInRuntimeInternalApi)

                function(setupFunctionName) {
                    addModifiers(KModifier.PRIVATE)
                    addParameter("instance", originalClass)
                    for (fn in setupFunctions) {
                        val functionName = fn.name.toString()
                        addStatement("instance.%N()", functionName)
                    }
                }

                function(teardownFunctionName) {
                    addModifiers(KModifier.PRIVATE)
                    addParameter("instance", originalClass)
                    for (fn in teardownFunctions) {
                        val functionName = fn.name.toString()
                        addStatement("instance.%N()", functionName)
                    }
                }

                function(parametersFunctionName) {
                    addModifiers(KModifier.PRIVATE)
                    addParameter("instance", originalClass)
                    addParameter("params", MAP.parameterizedBy(STRING, STRING))
                    
                    parameterProperties.forEach { property ->
                        val type = property.type.nameIfStandardType!!
                        addStatement("instance.${property.name} = params.getValue(\"${property.name}\").to$type()")
                    }
                }

                val defaultParameters = parameterProperties.associateBy({ it.name }, {
                    val annotation = it.annotations.findAnnotation(FqName(paramAnnotationFQN))!!
                    @Suppress("UNCHECKED_CAST")
                    annotation.argumentValue("value")!!.value as List<StringValue>
                })

                val defaultParametersString = defaultParameters.entries
                    .joinToString(prefix = "mapOf(", postfix = ")") { (key, value) ->
                        "\"${key}\" to ${value.joinToString(prefix = "listOf(", postfix = ")") { "\"\"\"${it.value.replace(' ', '·')}\"\"\"" }}"
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
                        val functionName = fn.name.toString()

                        val hasABlackholeParameter = fn.valueParameters.singleOrNull()?.type.toString() == "Blackhole"

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
val KotlinType.nameIfStandardType: Name?
    get() = constructor.declarationDescriptor?.name
