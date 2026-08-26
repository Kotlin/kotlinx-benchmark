package kotlinx.benchmark.klib

import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal fun KSAnnotated.annotationOrNull(fqn: String): KSAnnotation? = annotations.singleOrNull {
    it.annotationType.resolve().declaration.qualifiedName?.asString() == fqn
}

internal fun KSAnnotated.hasAnnotation(fqn: String): Boolean = annotationOrNull(fqn) != null

@Suppress("UNCHECKED_CAST")
internal fun <T> KSAnnotation.argumentValueOrNull(name: String): T? = arguments.singleOrNull {
    it.name?.getShortName() == name
}?.value as T?

internal class SuiteSourceGenerator(
    private val title: String,
    private val codeGenerator: CodeGenerator,
    private val platform: Platform
) {
    companion object {
        const val setupFunctionName = "setUp"
        const val teardownFunctionName = "tearDown"
        const val parametersFunctionName = "parametrize"

        const val suiteExecutorFQN = "kotlinx.benchmark.SuiteExecutorBase"
        const val benchmarkAnnotationFQN = "kotlinx.benchmark.Benchmark"
        const val setupAnnotationFQN = "kotlinx.benchmark.Setup"
        const val teardownAnnotationFQN = "kotlinx.benchmark.TearDown"
        const val stateAnnotationFQN = "kotlinx.benchmark.State"
        const val modeAnnotationFQN = "kotlinx.benchmark.BenchmarkMode"
        const val timeUnitFQN = "kotlinx.benchmark.BenchmarkTimeUnit"
        const val iterationTimeFQN = "kotlinx.benchmark.IterationTime"
        const val modeFQN = "kotlinx.benchmark.Mode"
        const val outputTimeAnnotationFQN = "kotlinx.benchmark.OutputTimeUnit"
        const val warmupAnnotationFQN = "kotlinx.benchmark.Warmup"
        const val measureAnnotationFQN = "kotlinx.benchmark.Measurement"
        const val paramAnnotationFQN = "kotlinx.benchmark.Param"

        const val blackholeFQN = "kotlinx.benchmark.Blackhole"
        const val mainBenchmarkPackage = "kotlinx.benchmark.generated"

        val suppressWarnings = AnnotationSpec.builder(Suppress::class).addMember(
            "\"UNUSED_PARAMETER\", \"REDUNDANT_CALL_OF_CONVERSION_METHOD\""
        ).build()
        val optInRuntimeInternalApi = AnnotationSpec.builder(ClassName("kotlin", "OptIn")).addMember(
            "kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi::class"
        ).build()
    }

    private val suiteDescriptorType = ClassName.bestGuess(platform.suiteDescriptorClass)
    private val benchmarks = mutableListOf<ClassName>()

    private val originatingFiles = mutableSetOf<KSFile>()

    fun generate(resolver: Resolver) {
        resolver
            .getSymbolsWithAnnotation(stateAnnotationFQN, false /* nested benchmark classes are not expected (?) */)
            .filterIsInstance<KSClassDeclaration>()
            .filter { !it.isAbstract() }
            .forEach { benchmarkClass ->
                benchmarkClass.containingFile?.let(originatingFiles::add)
                generateBenchmark(benchmarkClass)
            }
    }

    fun generateRunnerMain() {
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
        file.writeToNewFile(Dependencies(true, *originatingFiles.toTypedArray()))
    }

    private fun generateBenchmark(original: KSClassDeclaration) {
        val originalPackage = original.packageName.asString()
        val originalName = original.simpleName.getShortName()
        val originalClass = ClassName(originalPackage, originalName)

        val benchmarkPackageName = mainBenchmarkPackage + if (originalPackage.isNotEmpty()) ".$originalPackage" else ""
        val benchmarkName = "${originalName}_Descriptor"
        val benchmarkClass = ClassName(benchmarkPackageName, benchmarkName)

        val functions = original.getAllFunctions()
        val parameterProperties = original.getAllProperties()
            .filter { it.hasAnnotation(paramAnnotationFQN) }
            .toList()

        validateParameterProperties(parameterProperties)

        val measureAnnotation = original.annotationOrNull(measureAnnotationFQN)
        val warmupAnnotation = original.annotationOrNull(warmupAnnotationFQN)
        val outputTimeAnnotation = original.annotationOrNull(outputTimeAnnotationFQN)
        val modeAnnotation = original.annotationOrNull(modeAnnotationFQN)

        val outputTimeUnit = outputTimeAnnotation?.argumentValueOrNull<Any>("value")?.enumEntryName()
        val modesValue = modeAnnotation?.argumentValueOrNull<List<Any>>("value")
        val mode = modesValue?.singleOrNull()?.enumEntryName()
        val iterations = measureAnnotation?.argumentValueOrNull<Int>("iterations")
        val iterationTime = measureAnnotation?.argumentValueOrNull<Int>("time")
        val iterationTimeUnit = measureAnnotation?.argumentValueOrNull<Any>("timeUnit")?.enumEntryName() ?: "SECONDS"
        val warmups = warmupAnnotation?.argumentValueOrNull<Int>("iterations")

        val benchmarkFunctions = functions
            .filter { it.hasAnnotation(benchmarkAnnotationFQN) }
            .toList()
        validateBenchmarkFunctions(benchmarkFunctions)

        val setupFunctions = functions
            .filter { it.hasAnnotation(setupAnnotationFQN) }
            .toList()
        validateSetupFunctions(setupFunctions)

        val teardownFunctions = functions
            .filter { it.hasAnnotation(teardownAnnotationFQN) }
            .toList()
            .reversed()
        validateTeardownFunctions(teardownFunctions)

        val dependencies = Dependencies(false, *listOfNotNull(original.containingFile).toTypedArray())
        val file = FileSpec.builder(benchmarkPackageName, benchmarkName).apply {
            declareObject(benchmarkClass) {
                addAnnotation(suppressWarnings)
                addAnnotation(optInRuntimeInternalApi)

                function(setupFunctionName) {
                    addModifiers(KModifier.PRIVATE)
                    addParameter("instance", originalClass)
                    for (fn in setupFunctions) {
                        val functionName = fn.simpleName.asString()
                        addStatement("instance.%N()", functionName)
                    }
                }

                function(teardownFunctionName) {
                    addModifiers(KModifier.PRIVATE)
                    addParameter("instance", originalClass)
                    for (fn in teardownFunctions) {
                        val functionName = fn.simpleName.asString()
                        addStatement("instance.%N()", functionName)
                    }
                }

                function(parametersFunctionName) {
                    addModifiers(KModifier.PRIVATE)
                    addParameter("instance", originalClass)
                    addParameter("params", MAP.parameterizedBy(STRING, STRING))

                    parameterProperties.forEach { property ->
                        val propertyName = property.simpleName.asString()
                        val type = property.type.resolve().declaration.simpleName.asString()
                        addStatement("instance.$propertyName = params.getValue(%S).to$type()", propertyName)
                    }
                }

                val defaultParameters = parameterProperties.associateBy({ it.simpleName.asString() }, {
                    val annotation = it.annotationOrNull(paramAnnotationFQN)!!
                    annotation.argumentValueOrNull<List<String>>("value")!!
                })

                val defaultParametersString = defaultParameters.entries
                    .joinToString(prefix = "mapOf(", postfix = ")") { (key, value) ->
                        val joinedValues = value.joinToString {
                            "\"\"\"${it.replace(' ', '·')}\"\"\""
                        }
                        "\"$key\" to listOf($joinedValues)"
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
                        parameterProperties.joinToString(prefix = "listOf(", postfix = ")") {
                            "\"${it.simpleName.asString()}\""
                        }
                    addCode(", parameters = $params")
                    addCode(", defaultParameters = $defaultParametersString")

                    if (iterations != null)
                        addCode(", iterations = $iterations")
                    if (warmups != null)
                        addCode(", warmups = $warmups")
                    if (iterationTime != null)
                        addCode(
                            ", iterationTime = %T($iterationTime, %T.%N)",
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
                        val functionName = fn.simpleName.asString()
                        val hasABlackholeParameter =
                            fn.parameters.singleOrNull()?.type?.resolve()?.declaration?.qualifiedName?.asString() == blackholeFQN
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

        file.writeToNewFile(dependencies)
    }

    private fun FileSpec.writeToNewFile(dependencies: Dependencies) {
        codeGenerator.createNewFile(dependencies, packageName, name).bufferedWriter().use {
            writeTo(it)
        }
    }
}

private fun Any.enumEntryName(): String? = when (this) {
    is KSName -> getShortName()
    is KSClassDeclaration -> simpleName.asString()
    else -> toString().substringAfterLast('.').takeIf { it.isNotEmpty() }
}

private inline fun FileSpec.Builder.function(
    name: String,
    builderAction: FunSpec.Builder.() -> Unit
): FunSpec = FunSpec.builder(name).apply(builderAction).build().also {
    addFunction(it)
}

private inline fun FileSpec.Builder.declareObject(
    name: ClassName,
    builderAction: TypeSpec.Builder.() -> Unit
): TypeSpec = TypeSpec.objectBuilder(name).apply(builderAction).build().also {
    addType(it)
}

private inline fun TypeSpec.Builder.function(
    name: String,
    builderAction: FunSpec.Builder.() -> Unit
): FunSpec = FunSpec.builder(name).apply(builderAction).build().also {
    addFunction(it)
}

@Suppress("unused")
private inline fun TypeSpec.Builder.property(
    name: String,
    type: ClassName,
    builderAction: PropertySpec.Builder.() -> Unit
): PropertySpec = PropertySpec.builder(name, type).apply(builderAction).build().also {
    addProperty(it)
}
