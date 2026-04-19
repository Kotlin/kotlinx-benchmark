package kotlinx.benchmark.gradle

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.benchmark.gradle.SuiteSourceGenerator.Companion.measureAnnotationFQN
import kotlinx.benchmark.gradle.SuiteSourceGenerator.Companion.paramAnnotationFQN
import kotlinx.benchmark.gradle.SuiteSourceGenerator.Companion.setupAnnotationFQN
import kotlinx.benchmark.gradle.SuiteSourceGenerator.Companion.teardownAnnotationFQN
import kotlinx.benchmark.gradle.SuiteSourceGenerator.Companion.warmupAnnotationFQN
import kotlinx.benchmark.gradle.internal.generator.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import java.io.*
import java.util.*
import java.util.jar.*
import kotlin.collections.*

/**
 * Task for scanning benchmark classes in the AAR's classes.jar and generating the
 * Jetpack Microbenchmark source files needed to run them on Android.
 */
@OptIn(RequiresKotlinCompilerEmbeddable::class)
internal abstract class AndroidGeneratorTask : DefaultTask() {

    /**
     * Directory where the AAR file from the main project was unpacked.
     * Normally, it should be identical to [AndroidUnpackLibraryArtifactTask.unpackedAarDir].
     */
    @get:InputDirectory
    abstract val unpackedAarDir: DirectoryProperty

    @get:Input
    abstract val compilationName: Property<String>

    @get:Input
    abstract val targetName: Property<String>

    /**
     * Directory where empty Jetpack Microbenchmark Gradle was created. It should
     * point to the root of the project.
     *
     * See [AndroidSetupBenchmarkProject]
     */
    @get:OutputDirectory
    abstract val benchmarkProjectDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val classesJar = unpackedAarDir.get().asFile.resolve("classes.jar")
        if (classesJar.exists()) {
            logger.debug("Processing classes.jar for {}[{}]", targetName.get(), compilationName.get())
            val jar = JarFile(classesJar)
            val annotationProcessor = AnnotationProcessor()
            annotationProcessor.processJarFile(jar, logger)
            jar.close()

            val targetDir = benchmarkProjectDir.get().asFile
            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            targetDir.mkdirs()
            generateBenchmarkSourceFiles(targetDir, annotationProcessor.getClassDescriptors())
        } else {
            logger.warn("classes.jar not found in AAR file: {}", classesJar.absolutePath)
        }
    }

    private fun generateBenchmarkSourceFiles(
        targetDir: File,
        classDescriptors: List<ClassAnnotationsDescriptor>,
    ) {
        classDescriptors.forEach { descriptor ->
            if (descriptor.visibility == Visibility.PUBLIC && !descriptor.isAbstract) {
                if (descriptor.getSpecificField(paramAnnotationFQN).isNotEmpty()) {
                    generateParameterizedDescriptorFile(descriptor, targetDir)
                } else {
                    generateDescriptorFile(descriptor, targetDir)
                }
            }
        }
    }

    private fun generateDescriptorFile(descriptor: ClassAnnotationsDescriptor, androidTestDir: File) {
        // NOTE: Changes to the naming of test classes and files should be reflected in
        // `AndroidMulitplatformTasks.processDeviceBenchmarkResults()`
        val descriptorName = "${descriptor.name}_Descriptor"
        val packageName = descriptor.packageName
        val fileSpecBuilder = FileSpec.builder(packageName, descriptorName)
            .addSharedImports(descriptor)
            .addImport("androidx.test.ext.junit.runners", "AndroidJUnit4")

        val typeSpecBuilder = TypeSpec.classBuilder(descriptorName)
            .addAnnotation(
                AnnotationSpec.builder(ClassName("org.junit.runner", "RunWith"))
                    .addMember("%T::class", ClassName("androidx.test.ext.junit.runners", "AndroidJUnit4"))
                    .build()
            )
            .addProperty(createBenchmarkRuleProperty())

        val methodsAdded = addBenchmarkMethods(typeSpecBuilder, descriptor)
        fileSpecBuilder.addType(typeSpecBuilder.build())
        if (methodsAdded) {
            // Only generate the test class if it has benchmark files, otherwise the JUnit4Runner will crash
            fileSpecBuilder.build().writeTo(androidTestDir)
        }
    }

    // Add imports shared between normal and parameterized tests
    private fun FileSpec.Builder.addSharedImports(descriptor: ClassAnnotationsDescriptor): FileSpec.Builder {
        this.addImport("androidx.benchmark.junit4", "BenchmarkRule")
            .addImport("androidx.benchmark.junit4", "measureRepeated")
            .addImport("org.junit", "Rule")
            .addImport("org.junit", "Test")
            .addImport("org.junit.runner", "RunWith")

        if (descriptor.hasSetupOrTeardownMethods()) {
            this.addImport("org.junit", "Before")
                .addImport("org.junit", "After")
        }

        return this
    }

    private fun createBenchmarkRuleProperty(): PropertySpec =
        PropertySpec.builder("benchmarkRule", ClassName("androidx.benchmark.junit4", "BenchmarkRule"))
            .initializer("%T()", ClassName("androidx.benchmark.junit4", "BenchmarkRule"))
            .addAnnotation(
                AnnotationSpec.builder(ClassName("org.junit", "Rule"))
                    .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                    .build()
            )
            .build()

    private fun generateParameterizedDescriptorFile(descriptor: ClassAnnotationsDescriptor, androidTestDir: File) {
        val descriptorName = "${descriptor.name}_Descriptor"
        val packageName = descriptor.packageName
        val fileSpecBuilder = FileSpec.builder(packageName, descriptorName)
            .addSharedImports(descriptor)
            .addImport("org.junit.runners", "Parameterized")

        // Generate constructor
        val constructorSpec = FunSpec.constructorBuilder()
        val paramFields = descriptor.getSpecificField(paramAnnotationFQN)
        paramFields.forEach { param ->
            constructorSpec.addParameter(param.name, getTypeName(param.type))
        }

        val typeSpecBuilder = TypeSpec.classBuilder(descriptorName)
            .addAnnotation(
                AnnotationSpec.builder(ClassName("org.junit.runner", "RunWith"))
                    .addMember("%T::class", ClassName("org.junit.runners", "Parameterized"))
                    .build()
            )
            .primaryConstructor(constructorSpec.build())
            .addProperty(createBenchmarkRuleProperty())
            .addProperties(paramFields.map { param ->
                PropertySpec.builder(param.name, getTypeName(param.type))
                    .initializer(param.name)
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            })

        val methodsAdded = addBenchmarkMethods(typeSpecBuilder, descriptor, true)

        // Generate companion object with parameters
        val companionSpec = TypeSpec.companionObjectBuilder()
            .addFunction(generateParametersFunction(paramFields))
            .build()

        typeSpecBuilder.addType(companionSpec)

        fileSpecBuilder.addType(typeSpecBuilder.build())

        if (methodsAdded) {
            // Only generate the test class if it has benchmark files, otherwise the JUnit4Runner will crash
            fileSpecBuilder.build().writeTo(androidTestDir)
        }
    }

    private fun generateParametersFunction(paramFields: List<FieldAnnotationsDescriptor>): FunSpec {
        val dataFunctionBuilder = FunSpec.builder("data")
            .addAnnotation(JvmStatic::class)
            .returns(
                ClassName("kotlin.collections", "Collection")
                    .parameterizedBy(
                        ClassName("kotlin", "Array")
                            .parameterizedBy(ANY)
                    )
            )

        val paramNameAndIndex = paramFields.mapIndexed { index, param ->
            "${param.name}={${index}}"
        }.joinToString(", ")

        val paramAnnotationValue = "{index}: $paramNameAndIndex"

        dataFunctionBuilder.addAnnotation(
            AnnotationSpec.builder(ClassName("org.junit.runners", "Parameterized.Parameters"))
                .addMember("name = \"%L\"", paramAnnotationValue)
                .build()
        )

        val paramValueLists = paramFields.map { param ->
            val values = param.annotations
                .find { it.name == paramAnnotationFQN }
                ?.parameters?.get("value") as List<*>

            values.map { value ->
                if (param.type == "java.lang.String") {
                    "\"\"\"$value\"\"\""
                } else {
                    value.toString()
                }
            }
        }

        val cartesianProduct = cartesianProduct(paramValueLists as List<List<Any>>)

        val returnStatement = StringBuilder("return listOf(\n")
        cartesianProduct.forEachIndexed { index, combination ->
            val arrayContent = combination.joinToString(", ")
            returnStatement.append("    arrayOf($arrayContent)")
            if (index != cartesianProduct.size - 1) {
                returnStatement.append(",\n")
            }
        }
        returnStatement.append("\n)")
        dataFunctionBuilder.addStatement(returnStatement.toString())

        return dataFunctionBuilder.build()
    }

    private fun cartesianProduct(lists: List<List<Any>>): List<List<Any>> {
        if (lists.isEmpty()) return emptyList()
        return lists.fold(listOf(listOf())) { acc, list ->
            acc.flatMap { prefix -> list.map { value -> prefix + value } }
        }
    }

    private fun addBenchmarkMethods(
        typeSpecBuilder: TypeSpec.Builder,
        descriptor: ClassAnnotationsDescriptor,
        isParameterized: Boolean = false
    ): Boolean {
        val className = "${descriptor.packageName}.${descriptor.name}"
        val propertyName = descriptor.name.replaceFirstChar { it.lowercase(Locale.getDefault()) }

        typeSpecBuilder.addProperty(
            PropertySpec.builder(propertyName, ClassName.bestGuess(className))
                .initializer("%T()", ClassName.bestGuess(className))
                .addModifiers(KModifier.PRIVATE)
                .build()
        )

        // TODO: Handle methods with parameters (Blackhole)
        var hasTestMethods: Boolean
        descriptor.methods
            .filter { it.visibility == Visibility.PUBLIC && it.parameters.isEmpty() }
            .filterNot { method ->
                method.annotations.any { annotation -> annotation.name == paramAnnotationFQN }
            }
            .also { hasTestMethods = it.isNotEmpty() }
            .forEach { method ->
                when {
                    method.annotations.any { it.name == setupAnnotationFQN || it.name == teardownAnnotationFQN } -> {
                        generateNonMeasurableMethod(descriptor, method, propertyName, typeSpecBuilder)
                    }

                    isParameterized && descriptor.getSpecificField(paramAnnotationFQN).isNotEmpty() -> {
                        generateParameterizedMeasurableMethod(descriptor, method, propertyName, typeSpecBuilder)
                    }

                    else -> {
                        generateMeasurableMethod(descriptor, method, propertyName, typeSpecBuilder)
                    }
                }
            }

        return hasTestMethods
    }

    private fun generateCommonMeasurableMethod(
        descriptor: ClassAnnotationsDescriptor,
        method: MethodAnnotationsDescriptor,
        propertyName: String,
        typeSpecBuilder: TypeSpec.Builder,
        isParameterized: Boolean
    ) {

        // Measurement and Warmup iterations are currently ignored on Android as there
        // are semantic differences between what these mean on JMH and Android.
        // Keep them visible, so we do not forget to revisit them in the future.
        @Suppress("UnusedVariable")
        val measurementIterations = descriptor.annotations
            .find { it.name == measureAnnotationFQN }
            ?.parameters?.get("iterations") as? Int ?: 5
        @Suppress("UnusedVariable")
        val warmupIterations = descriptor.annotations
            .find { it.name == warmupAnnotationFQN }
            ?.parameters?.get("iterations") as? Int ?: 5

        val methodSpecBuilder = FunSpec.builder("benchmark_${descriptor.name}_${method.name}")
            .addAnnotation(ClassName("org.junit", "Test"))

        if (isParameterized) {
            descriptor.getSpecificField(paramAnnotationFQN).forEach { field ->
                methodSpecBuilder.addStatement("$propertyName.${field.name} = ${field.name}")
            }
        }

        methodSpecBuilder
            .beginControlFlow("benchmarkRule.measureRepeated")
            .addStatement("$propertyName.${method.name}()")
            .endControlFlow()

        typeSpecBuilder.addFunction(methodSpecBuilder.build())
    }

    private fun generateParameterizedMeasurableMethod(
        descriptor: ClassAnnotationsDescriptor,
        method: MethodAnnotationsDescriptor,
        propertyName: String,
        typeSpecBuilder: TypeSpec.Builder
    ) {
        generateCommonMeasurableMethod(descriptor, method, propertyName, typeSpecBuilder, isParameterized = true)
    }

    private fun generateMeasurableMethod(
        descriptor: ClassAnnotationsDescriptor,
        method: MethodAnnotationsDescriptor,
        propertyName: String,
        typeSpecBuilder: TypeSpec.Builder
    ) {
        generateCommonMeasurableMethod(descriptor, method, propertyName, typeSpecBuilder, isParameterized = false)
    }


    private fun generateNonMeasurableMethod(
        descriptor: ClassAnnotationsDescriptor,
        method: MethodAnnotationsDescriptor,
        propertyName: String,
        typeSpecBuilder: TypeSpec.Builder
    ) {
        when (method.annotations.first().name) {
            setupAnnotationFQN -> {
                val methodSpecBuilder = FunSpec.builder("benchmark_${descriptor.name}_setUp")
                    .addAnnotation(ClassName("org.junit", "Before"))
                    .addStatement("$propertyName.${method.name}()")
                typeSpecBuilder.addFunction(methodSpecBuilder.build())
            }

            teardownAnnotationFQN -> {
                val methodSpecBuilder = FunSpec.builder("benchmark_${descriptor.name}_tearDown")
                    .addAnnotation(ClassName("org.junit", "After"))
                    .addStatement("$propertyName.${method.name}()")
                typeSpecBuilder.addFunction(methodSpecBuilder.build())
            }
        }
    }

    private fun getTypeName(type: String): TypeName {
        return when (type) {
            "int" -> Int::class.asTypeName()
            "long" -> Long::class.asTypeName()
            "boolean" -> Boolean::class.asTypeName()
            "float" -> Float::class.asTypeName()
            "double" -> Double::class.asTypeName()
            "char" -> Char::class.asTypeName()
            "byte" -> Byte::class.asTypeName()
            "short" -> Short::class.asTypeName()
            "java.lang.String" -> String::class.asTypeName()
            else -> ClassName.bestGuess(type)
        }
    }
}
