@file:OptIn(RequiresKotlinCompilerEmbeddable::class)

package kotlinx.benchmark.gradle

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.benchmark.gradle.SuiteSourceGenerator.Companion.measureAnnotationFQN
import kotlinx.benchmark.gradle.SuiteSourceGenerator.Companion.paramAnnotationFQN
import kotlinx.benchmark.gradle.SuiteSourceGenerator.Companion.setupAnnotationFQN
import kotlinx.benchmark.gradle.SuiteSourceGenerator.Companion.teardownAnnotationFQN
import kotlinx.benchmark.gradle.SuiteSourceGenerator.Companion.warmupAnnotationFQN
import kotlinx.benchmark.gradle.internal.generator.RequiresKotlinCompilerEmbeddable
import java.io.File
import java.util.*

internal fun generateBenchmarkSourceFiles(
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
    val descriptorName = "${descriptor.name}_Descriptor"
    val packageName = descriptor.packageName
    val fileSpecBuilder = FileSpec.builder(packageName, descriptorName)
        .addImport("androidx.test.ext.junit.runners", "AndroidJUnit4")
        .addImport("org.junit", "Test")
        .addImport("org.junit", "Before")
        .addImport("org.junit", "After")
        .addImport("org.junit.runner", "RunWith")
        .addImport("androidx.benchmark", "BenchmarkState")
        .addImport("androidx.benchmark", "ExperimentalBenchmarkStateApi")
        .addImport("android.util", "Log")

    if (descriptor.hasSetupOrTeardownMethods()) {
        fileSpecBuilder
            .addImport("org.junit", "Before")
            .addImport("org.junit", "After")
    }

    val typeSpecBuilder = TypeSpec.classBuilder(descriptorName)
        .addAnnotation(
            AnnotationSpec.builder(ClassName("org.junit.runner", "RunWith"))
                .addMember("%T::class", ClassName("androidx.test.ext.junit.runners", "AndroidJUnit4"))
                .build()
        )

    addBenchmarkMethods(typeSpecBuilder, descriptor)

    fileSpecBuilder.addType(typeSpecBuilder.build())
    fileSpecBuilder.build().writeTo(androidTestDir)
}

private fun generateParameterizedDescriptorFile(descriptor: ClassAnnotationsDescriptor, androidTestDir: File) {
    val descriptorName = "${descriptor.name}_Descriptor"
    val packageName = descriptor.packageName
    val fileSpecBuilder = FileSpec.builder(packageName, descriptorName)
        .addImport("org.junit.runner", "RunWith")
        .addImport("org.junit.runners", "Parameterized")
        .addImport("androidx.benchmark", "BenchmarkState")
        .addImport("androidx.benchmark", "ExperimentalBenchmarkStateApi")
        .addImport("org.junit", "Test")
        .addImport("android.util", "Log")

    if (descriptor.hasSetupOrTeardownMethods()) {
        fileSpecBuilder
            .addImport("org.junit", "Before")
            .addImport("org.junit", "After")
    }

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
        .addProperties(paramFields.map { param ->
            PropertySpec.builder(param.name, getTypeName(param.type))
                .initializer(param.name)
                .addModifiers(KModifier.PRIVATE)
                .build()
        })

    addBenchmarkMethods(typeSpecBuilder, descriptor, true)

    // Generate companion object with parameters
    val companionSpec = TypeSpec.companionObjectBuilder()
        .addFunction(generateParametersFunction(paramFields))
        .build()

    typeSpecBuilder.addType(companionSpec)

    fileSpecBuilder.addType(typeSpecBuilder.build())
    fileSpecBuilder.build().writeTo(androidTestDir)
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
    return lists.fold(listOf(listOf<Any>())) { acc, list ->
        acc.flatMap { prefix -> list.map { value -> prefix + value } }
    }
}

private fun addBenchmarkMethods(
    typeSpecBuilder: TypeSpec.Builder,
    descriptor: ClassAnnotationsDescriptor,
    isParameterized: Boolean = false
) {
    val className = "${descriptor.packageName}.${descriptor.name}"
    val propertyName = descriptor.name.decapitalize(Locale.getDefault())

    typeSpecBuilder.addProperty(
        PropertySpec.builder(propertyName, ClassName.bestGuess(className))
            .initializer("%T()", ClassName.bestGuess(className))
            .addModifiers(KModifier.PRIVATE)
            .build()
    )

    // TODO: Handle methods with parameters (Blackhole)
    descriptor.methods
        .filter { it.visibility == Visibility.PUBLIC && it.parameters.isEmpty() }
        .filterNot { method ->
            method.annotations.any { annotation -> annotation.name == paramAnnotationFQN }
        }
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
}

private fun generateCommonMeasurableMethod(
    descriptor: ClassAnnotationsDescriptor,
    method: MethodAnnotationsDescriptor,
    propertyName: String,
    typeSpecBuilder: TypeSpec.Builder,
    isParameterized: Boolean
) {
    val measurementIterations = descriptor.annotations
        .find { it.name == measureAnnotationFQN }
        ?.parameters?.get("iterations") as? Int ?: 5
    val warmupIterations = descriptor.annotations
        .find { it.name == warmupAnnotationFQN }
        ?.parameters?.get("iterations") as? Int ?: 5

    val methodSpecBuilder = FunSpec.builder("benchmark_${descriptor.name}_${method.name}")
        .addAnnotation(ClassName("org.junit", "Test"))
        .addAnnotation(
            AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                .addMember("%T::class", ClassName("androidx.benchmark", "ExperimentalBenchmarkStateApi"))
                .build()
        )

    if (isParameterized) {
        descriptor.getSpecificField(paramAnnotationFQN).forEach { field ->
            methodSpecBuilder.addStatement("$propertyName.${field.name} = ${field.name}")
        }
    }

    methodSpecBuilder
        .addStatement(
            "val state = %T(warmupCount = $warmupIterations, repeatCount = $measurementIterations)",
            ClassName("androidx.benchmark", "BenchmarkState")
        )
        .beginControlFlow("while (state.keepRunning())")
        .addStatement("$propertyName.${method.name}()")
        .endControlFlow()
        .addStatement("val measurementResult = state.getMeasurementTimeNs()")
        .beginControlFlow("measurementResult.forEachIndexed { index, time ->")
        .addStatement("Log.d(\"KotlinBenchmark\", \"Iteration \${index + 1}: \$time ns\")")
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