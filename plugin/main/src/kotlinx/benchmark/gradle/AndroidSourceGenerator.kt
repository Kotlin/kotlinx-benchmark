package kotlinx.benchmark.gradle

import com.squareup.kotlinpoet.*
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.*
import java.io.File
import java.util.*

@KotlinxBenchmarkPluginInternalApi
fun Project.generateBenchmarkSourceFiles(
    classDescriptors: List<ClassAnnotationsDescriptor>,
) {

    // TODO: Path needs to generate files
    val targetPath = "E:/Android/AndroidProjects/kotlin-qualification-task/microbenchmark"
    val androidTestDir = File(targetPath).resolve("src/androidTest/kotlin")
    if (!androidTestDir.exists()) {
        androidTestDir.mkdirs()
    }

    val buildGradleFile = File(targetPath).resolve("build.gradle.kts")
    val dependencyPaths = listOf(
        "${project.projectDir}/build/outputs/unpacked-aar/release/classes.jar".replace("\\", "/") to null,
        "androidx.benchmark:benchmark-junit4" to "1.2.4",
        "androidx.test.ext:junit-ktx" to "1.2.1",
        "junit:junit" to "4.13.2"
    )

    updateAndroidDependencies(buildGradleFile, dependencyPaths)

    classDescriptors.forEach { descriptor ->
        if (descriptor.visibility == Visibility.PUBLIC && !descriptor.isAbstract) {
            generateDescriptorFile(descriptor, androidTestDir)
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

private fun addBenchmarkMethods(typeSpecBuilder: TypeSpec.Builder, descriptor: ClassAnnotationsDescriptor) {
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
            method.annotations.any { annotation -> annotation.name == "kotlinx.benchmark.Param" }
        }
        .forEach { method ->
            when {
                method.annotations.any { it.name == "kotlinx.benchmark.Setup" || it.name == "kotlinx.benchmark.TearDown" } -> {
                    generateNonMeasurableMethod(descriptor, method, propertyName, typeSpecBuilder)
                }
                else -> {
                    generateMeasurableMethod(descriptor, method, propertyName, typeSpecBuilder)
                }
            }
        }
}

private fun generateMeasurableMethod(
    descriptor: ClassAnnotationsDescriptor,
    method: MethodAnnotationsDescriptor,
    propertyName: String,
    typeSpecBuilder: TypeSpec.Builder
) {
    val measurementIterations = descriptor.annotations
        .find { it.name == "kotlinx.benchmark.Measurement" }
        ?.parameters?.get("iterations") as? Int ?: 5
    val warmupIterations = descriptor.annotations
        .find { it.name == "kotlinx.benchmark.Warmup" }
        ?.parameters?.get("iterations") as? Int ?: 5

    val methodSpecBuilder = FunSpec.builder("benchmark_${descriptor.name}_${method.name}")
        .addAnnotation(ClassName("org.junit", "Test"))
        .addAnnotation(
            AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                .addMember("%T::class", ClassName("androidx.benchmark", "ExperimentalBenchmarkStateApi"))
                .build()
        )
        // TODO: Add warmupCount and repeatCount parameters
        .addStatement(
            "val state = %T(warmupCount = $warmupIterations, repeatCount = $measurementIterations)",
            ClassName("androidx.benchmark", "BenchmarkState")
        )
        .beginControlFlow("while (state.keepRunning())")
        .addStatement("$propertyName.${method.name}()")
        .endControlFlow()
        .addStatement("val measurementResult = state.getMeasurementTimeNs()")
        .beginControlFlow("measurementResult.forEachIndexed { index, time ->")
        .addStatement("println(\"Iteration \${index + 1}: \$time ns\")")
        .endControlFlow()
    typeSpecBuilder.addFunction(methodSpecBuilder.build())
}

private fun generateNonMeasurableMethod(
    descriptor: ClassAnnotationsDescriptor,
    method: MethodAnnotationsDescriptor,
    propertyName: String,
    typeSpecBuilder: TypeSpec.Builder
) {
    when (method.annotations.first().name) {
        "kotlinx.benchmark.Setup" -> {
            val methodSpecBuilder = FunSpec.builder("benchmark_${descriptor.name}_setUp")
                .addAnnotation(ClassName("org.junit", "Before"))
                .addStatement("$propertyName.${method.name}()")
            typeSpecBuilder.addFunction(methodSpecBuilder.build())
        }

        "kotlinx.benchmark.TearDown" -> {
            val methodSpecBuilder = FunSpec.builder("benchmark_${descriptor.name}_tearDown")
                .addAnnotation(ClassName("org.junit", "After"))
                .addStatement("$propertyName.${method.name}()")
            typeSpecBuilder.addFunction(methodSpecBuilder.build())
        }
    }
}

private fun updateAndroidDependencies(buildGradleFile: File, dependencies: List<Pair<String, String?>>) {
    if (buildGradleFile.exists()) {
        val buildGradleContent = buildGradleFile.readText()

        if (buildGradleContent.contains("android {")) {
            val androidBlockStart = buildGradleContent.indexOf("android {")
            val androidBlockEnd = buildGradleContent.lastIndexOf("}") + 1
            val androidBlockContent = buildGradleContent.substring(androidBlockStart, androidBlockEnd)

            val newDependencies = dependencies.filterNot { (dependency, version) ->
                val dependencyString = version?.let { """$dependency:$version""" } ?: dependency
                androidBlockContent.contains(dependencyString)
            }
            if (newDependencies.isNotEmpty()) {
                val updatedAndroidBlockContent = if (androidBlockContent.contains("dependencies {")) {
                    val dependenciesBlockStart = androidBlockContent.indexOf("dependencies {")
                    val dependenciesBlockEnd = androidBlockContent.indexOf("}", dependenciesBlockStart) + 1
                    val dependenciesBlockContent =
                        androidBlockContent.substring(dependenciesBlockStart, dependenciesBlockEnd)

                    val newDependenciesString = newDependencies.joinToString("\n        ") { (dependency, version) ->
                        version?.let { """androidTestImplementation("$dependency:$version")""" }
                            ?: """androidTestImplementation(files("$dependency"))"""
                    }
                    androidBlockContent.replace(
                        dependenciesBlockContent,
                        dependenciesBlockContent.replace(
                            "dependencies {",
                            "dependencies {\n        $newDependenciesString"
                        )
                    )
                } else {
                    val newDependenciesString = newDependencies.joinToString("\n        ") { (dependency, version) ->
                        version?.let { """androidTestImplementation("$dependency:$version")""" }
                            ?: """androidTestImplementation(files("$dependency"))"""
                    }
                    androidBlockContent.replace("{", "{\n    dependencies {\n        $newDependenciesString\n    }\n")
                }

                val updatedBuildGradleContent =
                    buildGradleContent.replace(androidBlockContent, updatedAndroidBlockContent)
                buildGradleFile.writeText(updatedBuildGradleContent)
            }
        }
    }
}
