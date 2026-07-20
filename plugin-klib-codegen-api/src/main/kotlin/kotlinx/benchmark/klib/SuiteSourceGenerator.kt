package kotlinx.benchmark.klib

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.metadata.klib.KlibModuleMetadata
import kotlinx.metadata.klib.annotations
import java.io.File
import kotlin.collections.orEmpty
import kotlin.metadata.KmAnnotation
import kotlin.metadata.KmAnnotationArgument
import kotlin.metadata.KmClass
import kotlin.metadata.KmClassifier
import kotlin.metadata.KmType
import kotlin.metadata.modality

public enum class Platform(
    internal val runBenchmarks: String,
    internal val suiteDescriptorClass: String,
    internal val benchmarkDescriptorClass: String,
    internal val benchmarkDescriptorWithBlackholeParameterClass: String
) {
    JsBuiltIn(
        runBenchmarks = "kotlinx.benchmark.js.runBenchmarksBuiltIn",
        suiteDescriptorClass = "kotlinx.benchmark.SuiteDescriptor",
        benchmarkDescriptorClass = "kotlinx.benchmark.js.JsBenchmarkDescriptorWithNoBlackholeParameter",
        benchmarkDescriptorWithBlackholeParameterClass = "kotlinx.benchmark.js.JsBenchmarkDescriptorWithBlackholeParameter",
    ),
    JsBenchmarkJs(
        runBenchmarks = "kotlinx.benchmark.js.runBenchmarks",
        suiteDescriptorClass = "kotlinx.benchmark.SuiteDescriptor",
        benchmarkDescriptorClass = "kotlinx.benchmark.js.JsBenchmarkDescriptorWithNoBlackholeParameter",
        benchmarkDescriptorWithBlackholeParameterClass = "kotlinx.benchmark.js.JsBenchmarkDescriptorWithBlackholeParameter",
    ),
    NativeBuiltIn(
        runBenchmarks = "kotlinx.benchmark.native.runBenchmarks",
        suiteDescriptorClass = "kotlinx.benchmark.SuiteDescriptor",
        benchmarkDescriptorClass = "kotlinx.benchmark.BenchmarkDescriptorWithNoBlackholeParameter",
        benchmarkDescriptorWithBlackholeParameterClass = "kotlinx.benchmark.BenchmarkDescriptorWithBlackholeParameter",
    ),
    WasmBuiltIn(
        runBenchmarks = "kotlinx.benchmark.wasm.runBenchmarks",
        suiteDescriptorClass = "kotlinx.benchmark.SuiteDescriptor",
        benchmarkDescriptorClass = "kotlinx.benchmark.BenchmarkDescriptorWithNoBlackholeParameter",
        benchmarkDescriptorWithBlackholeParameterClass = "kotlinx.benchmark.BenchmarkDescriptorWithBlackholeParameter",
    )
}

public class SuiteSourceGenerator(
    internal val title: String,
    internal val module: KlibModule,
    internal val output: File,
    internal val platform: Platform
) {

    internal companion object {
        val setupFunctionName = "setUp"
        val teardownFunctionName = "tearDown"
        val parametersFunctionName = "parametrize"

        val suiteExecutorFQN = "kotlinx.benchmark.SuiteExecutorBase"
        val externalConfigurationFQN = "kotlinx.benchmark.ExternalConfiguration"
        val benchmarkAnnotationClassName = "kotlinx/benchmark/Benchmark"
        val setupAnnotationClassName = "kotlinx/benchmark/Setup"
        val teardownAnnotationClassName = "kotlinx/benchmark/TearDown"
        val stateAnnotationClassName = "kotlinx/benchmark/State"
        val modeAnnotationClassName = "kotlinx/benchmark/BenchmarkMode"
        val timeUnitFQN = "kotlinx.benchmark.BenchmarkTimeUnit"
        val iterationTimeFQN = "kotlinx.benchmark.IterationTime"
        val modeFQN = "kotlinx.benchmark.Mode"
        val outputTimeAnnotationClassName = "kotlinx/benchmark/OutputTimeUnit"
        val warmupAnnotationClassName = "kotlinx/benchmark/Warmup"
        val measureAnnotationClassName = "kotlinx/benchmark/Measurement"
        val paramAnnotationClassName = "kotlinx/benchmark/Param"

        val blackholeFQN = "kotlinx.benchmark.Blackhole"

        val mainBenchmarkPackage = "kotlinx.benchmark.generated"

        val suppressWarnings = AnnotationSpec.builder(Suppress::class).addMember(
            "\"UNUSED_PARAMETER\", \"REDUNDANT_CALL_OF_CONVERSION_METHOD\""
        ).build()
        val optInRuntimeInternalApi = AnnotationSpec.builder(ClassName("kotlin", "OptIn")).addMember(
            "kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi::class"
        ).build()
    }

    private val suiteDescriptorType = ClassName.bestGuess(platform.suiteDescriptorClass)

    private val benchmarks = mutableListOf<ClassName>()

    public fun generate() {
        processPackage(module.metadata)
        generateRunnerMain()
    }

    private fun generateRunnerMain() {
        val file = FileSpec.builder(mainBenchmarkPackage, "BenchmarkSuite").apply {
            function("main") {
                addAnnotation(optInRuntimeInternalApi)
                val array = ClassName("kotlin", "Array")
                val arrayOfStrings = array.parameterizedBy(WildcardTypeName.producerOf(String::class))
                addParameter("args", arrayOfStrings)
                addStatement("${platform.runBenchmarks}(%S, args, ::declareAndExecuteSuites)", title)
            }

            function("declareAndExecuteSuites") {
                addAnnotation(optInRuntimeInternalApi)
                val suiteExecutorClass = ClassName.bestGuess(suiteExecutorFQN)
                addParameter("executor", suiteExecutorClass)
                for (benchmark in benchmarks) {
                    addStatement("executor.suite(%T.describe())", benchmark)
                }
                addStatement("executor.run()")
            }
        }.build()
        file.writeTo(output)
    }

    private fun processPackage(metadata: KlibModuleMetadata) {
        for (moduleFragment in metadata.fragments) {
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

        val measureAnnotation = original.annotations.annotation(measureAnnotationClassName)
        val warmupAnnotation = original.annotations.annotation(warmupAnnotationClassName)
        val outputTimeAnnotation = original.annotations.annotation(outputTimeAnnotationClassName)
        val modeAnnotation = original.annotations.annotation(modeAnnotationClassName)

        val outputTimeUnit = outputTimeAnnotation?.singleEnumArgument("value")

        val mode = modeAnnotation?.enumArrayArgument("value")?.singleOrNull()

        val measureIterations = measureAnnotation?.intArgument("iterations")
        val measureIterationTime = measureAnnotation?.intArgument("time")
        val measureIterationTimeUnit = measureAnnotation?.singleEnumArgument("timeUnit")

        val warmupIterations = warmupAnnotation?.intArgument("iterations")

        val iterations = measureIterations
        val iterationTime = measureIterationTime
        val iterationTimeUnit = measureIterationTimeUnit ?: "SECONDS"
        val warmups = warmupIterations

        val benchmarkFunctions =
            functions.filter { it.annotations.annotation(benchmarkAnnotationClassName) != null }

        validateBenchmarkFunctions(benchmarkFunctions)

        val setupFunctions = functions
            .filter { it.annotations.annotation(setupAnnotationClassName) != null }

        validateSetupFunctions(setupFunctions)

        val teardownFunctions = functions
            .filter { it.annotations.annotation(teardownAnnotationClassName) != null }.reversed()

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
                        val type = property.returnType.renderTypeName()
                        addStatement("instance.${property.name} = params.getValue(\"${property.name}\").to$type()")
                    }
                }

                val defaultParameters = parameterProperties.associateBy({ it.name }, {
                    it.annotations.annotation(paramAnnotationClassName)!!.stringArrayArgument("value")
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
                            (fn.valueParameters.singleOrNull()?.type?.classifier as? KmClassifier.Class)?.name == "kotlinx/benchmark/Blackhole"

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

private inline fun codeBlock(builderAction: CodeBlock.Builder.() -> Unit): CodeBlock {
    return CodeBlock.builder().apply(builderAction).build()
}

private inline fun FileSpec.Builder.declareObject(name: ClassName, builderAction: TypeSpec.Builder.() -> Unit): TypeSpec {
    return TypeSpec.objectBuilder(name).apply(builderAction).build().also {
        addType(it)
    }
}

private inline fun TypeSpec.Builder.function(
    name: String,
    builderAction: FunSpec.Builder.() -> Unit
): FunSpec {
    return FunSpec.builder(name).apply(builderAction).build().also {
        addFunction(it)
    }
}

private inline fun FileSpec.Builder.function(
    name: String,
    builderAction: FunSpec.Builder.() -> Unit
): FunSpec {
    return FunSpec.builder(name).apply(builderAction).build().also {
        addFunction(it)
    }
}

internal fun Iterable<KmAnnotation>.annotation(fqName: String): KmAnnotation? {
    return firstOrNull { it.className == fqName }
}

internal fun KmAnnotation.arrayArgument(name: String): List<KmAnnotationArgument> {
    return (arguments[name] as? KmAnnotationArgument.ArrayValue)?.elements.orEmpty()
}

internal fun KmAnnotation.intArgument(name: String): Int? {
    return (arguments[name] as? KmAnnotationArgument.IntValue)?.value
}

internal fun KmAnnotation.singleEnumArgument(name: String): String? {
    return (arguments[name] as? KmAnnotationArgument.EnumValue)?.enumEntryName
}

internal fun KmAnnotation.enumArrayArgument(name: String): List<String> {
    return arrayArgument(name).mapNotNull { (it as? KmAnnotationArgument.EnumValue)?.enumEntryName }
}

internal fun KmAnnotation.stringArrayArgument(name: String): List<String> {
    return arrayArgument(name).mapNotNull { (it as? KmAnnotationArgument.StringValue)?.value }
}

internal fun KmType.className(): String? = (classifier as? KmClassifier.Class)?.name

internal fun KmType.renderTypeName(): String = when(val name = className()?.replace('/', '.') ?: toString()) {
    "kotlin.Byte" -> "Byte"
    "kotlin.Short" -> "Short"
    "kotlin.Int" -> "Int"
    "kotlin.Long" -> "Long"
    "kotlin.Float" -> "Float"
    "kotlin.Double" -> "Double"
    "kotlin.String" -> "String"
    "kotlin.Boolean" -> "Boolean"
    "kotlin.Char" -> "Char"
    "kotlin.UByte" -> "UByte"
    "kotlin.UInt" -> "UInt"
    "kotlin.ULong" -> "ULong"
    "kotlin.UShort" -> "UShort"
    else -> name
}
