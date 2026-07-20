package kotlinx.benchmark.gradle

import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.WildcardTypeName

internal fun KSAnnotated.annotationOrNull(fqn: String): KSAnnotation? = annotations.singleOrNull {
    it.annotationType.resolve().declaration.qualifiedName?.asString()?.equals(fqn) == true
}

internal fun KSAnnotated.hasAnnotation(fqn: String): Boolean = annotationOrNull(fqn) != null

@Suppress("UNCHECKED_CAST")
internal fun <T> KSAnnotation.argumentValueOrNull(name: String): T? = arguments.singleOrNull {
    it.name?.getShortName() == name
}?.value as T?

internal class SuiteSourceGeneratorWithKSP(
    val title: String,
    val codeGenerator: CodeGenerator,
    val platform: Platform
) {

    companion object {
        val setupFunctionName = "setUp"
        val teardownFunctionName = "tearDown"
        val parametersFunctionName = "parametrize"

        val suiteExecutorFQN = "kotlinx.benchmark.SuiteExecutorBase"
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

        val suppressWarnings = AnnotationSpec.builder(Suppress::class).addMember(
            "\"UNUSED_PARAMETER\", \"REDUNDANT_CALL_OF_CONVERSION_METHOD\""
        ).build()
        val optInRuntimeInternalApi = AnnotationSpec.builder(ClassName("kotlin", "OptIn")).addMember(
            "kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi::class"
        ).build()
    }

    private val suiteDescriptorType = ClassName.bestGuess(platform.suiteDescriptorClass)

    val benchmarks = mutableListOf<ClassName>()

    fun generate(resolver: Resolver) {
        resolver
            .getSymbolsWithAnnotation(stateAnnotationFQN, true)
            .filterIsInstance<KSClassDeclaration>()
            .filter { !it.isAbstract() }
            .forEach {
                generateBenchmark(it)
            }
    }

    private fun FileSpec.writeToNewFile(dependencies: Dependencies) = codeGenerator.createNewFile(
        dependencies,
        packageName,
        name,
    ).bufferedWriter().use {
        writeTo(it)
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
        file.writeToNewFile(Dependencies.ALL_FILES)
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

        val outputTimeUnitValue = outputTimeAnnotation?.argumentValueOrNull<KSClassDeclaration>("value")
        val outputTimeUnit = outputTimeUnitValue?.simpleName?.asString()

        @Suppress("UNCHECKED_CAST")
        val modesValue = modeAnnotation?.argumentValueOrNull<List<KSClassDeclaration>>("value")
        val mode = modesValue?.single()?.simpleName?.asString()

        val measureIterations = measureAnnotation?.argumentValueOrNull<Int>("iterations")
        val measureIterationTime = measureAnnotation?.argumentValueOrNull<Int>("time")
        val measureIterationTimeUnit = measureAnnotation?.argumentValueOrNull<KSClassDeclaration>("timeUnit")

        val warmupIterations = warmupAnnotation?.argumentValueOrNull<Int>("iterations")

        val iterations = measureIterations
        val iterationTime = measureIterationTime
        val iterationTimeUnit = measureIterationTimeUnit?.simpleName?.asString() ?: "SECONDS"
        val warmups = warmupIterations

        val benchmarkFunctions =
            functions.filter { it.hasAnnotation(benchmarkAnnotationFQN) }
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
                        val type = property.type.resolve().declaration.simpleName.asString()
                        addStatement("instance.${property.simpleName.asString()} = params.getValue(\"${property.simpleName.asString()}\").to$type()")
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
                        parameterProperties.joinToString(prefix = "listOf(", postfix = ")") { "\"${it.simpleName.asString()}\"" }
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
                        val functionName = fn.simpleName.asString()

                        val hasABlackholeParameter = fn.parameters.singleOrNull()?.type?.resolve()?.declaration?.simpleName?.asString() == "Blackhole"

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

        file.members

        file.writeToNewFile(Dependencies.ALL_FILES)
    }
}