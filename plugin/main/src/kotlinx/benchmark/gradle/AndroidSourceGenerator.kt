package kotlinx.benchmark.gradle

import com.squareup.kotlinpoet.*
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.*
import java.io.File

@KotlinxBenchmarkPluginInternalApi
fun Project.generateBenchmarkSourceFiles(
    classDescriptors: List<ClassAnnotationsDescriptor>,
) {

    val targetPath = "E:/Android/AndroidProjects/kotlin-qualification-task/composeApp"
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
        .addImport("org.junit.runner", "RunWith")
        .addImport("androidx.benchmark.junit4", "BenchmarkRule")

    val typeSpecBuilder = TypeSpec.classBuilder(descriptorName)
        .addAnnotation(
            AnnotationSpec.builder(ClassName("org.junit.runner", "RunWith"))
                .addMember("%T::class", ClassName("androidx.test.ext.junit.runners", "AndroidJUnit4"))
                .build()
        )
        .addProperty(
            PropertySpec.builder("benchmarkRule", ClassName("androidx.benchmark.junit4", "BenchmarkRule"))
                .addAnnotation(
                    AnnotationSpec.builder(ClassName("org.junit", "Rule"))
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                        .build()
                )
                .initializer("BenchmarkRule()")
                .build()
        )

    addBenchmarkMethods(typeSpecBuilder, descriptor)

    fileSpecBuilder.addType(typeSpecBuilder.build())
    fileSpecBuilder.build().writeTo(androidTestDir)
}

private fun addBenchmarkMethods(typeSpecBuilder: TypeSpec.Builder, descriptor: ClassAnnotationsDescriptor) {
    val className = "${descriptor.packageName}.${descriptor.name}"
    val propertyName = descriptor.name.decapitalize()

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
            val methodSpecBuilder = FunSpec.builder("benchmark_${descriptor.name}_${method.name}")
                .addAnnotation(ClassName("org.junit", "Test"))
                .addStatement("$propertyName.${method.name}()")
            typeSpecBuilder.addFunction(methodSpecBuilder.build())
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
                    val dependenciesBlockContent = androidBlockContent.substring(dependenciesBlockStart, dependenciesBlockEnd)

                    val newDependenciesString = newDependencies.joinToString("\n        ") { (dependency, version) ->
                        version?.let { """androidTestImplementation("$dependency:$version")""" } ?: """androidTestImplementation(files("$dependency"))"""
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
                        version?.let { """androidTestImplementation("$dependency:$version")""" } ?: """androidTestImplementation(files("$dependency"))"""
                    }
                    androidBlockContent.replace("{", "{\n    dependencies {\n        $newDependenciesString\n    }\n")
                }

                val updatedBuildGradleContent = buildGradleContent.replace(androidBlockContent, updatedAndroidBlockContent)
                buildGradleFile.writeText(updatedBuildGradleContent)
            }
        }
    }
}
