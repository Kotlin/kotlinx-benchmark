package kotlinx.benchmark.gradle

import com.squareup.kotlinpoet.*
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import java.io.File
import java.util.jar.JarFile

@KotlinxBenchmarkPluginInternalApi
fun Project.unpackAndProcessAar(target: KotlinJvmAndroidCompilation) {
    println("Unpacking AAR file for ${target.name}")
    val aarFile = File("${project.projectDir}/build/outputs/aar/${project.name}-${target.name}.aar")
    if (aarFile.exists()) {
        val unpackedDir = File("${project.projectDir}/build/outputs/unpacked-aar/${target.name}")
        val classesJar = File(unpackedDir, "classes.jar")

        // Unpack AAR file
        project.copy {
            it.from(project.zipTree(aarFile))
            it.into(unpackedDir)
        }

        if (classesJar.exists()) {
            println("Processing classes.jar for ${target.name}")
            val jar = JarFile(classesJar)
            val annotationProcessor = AnnotationProcessor()
            annotationProcessor.processJarFile(jar)
            jar.close()

            val classAnnotationsDescriptors = annotationProcessor.getClassDescriptors()
            println("Class annotations for ${target.name}:")

            classAnnotationsDescriptors.forEach { descriptor ->
                println("Package: ${descriptor.packageName}")
                println("Class: ${descriptor.name}")
                println("  Visibility: ${descriptor.visibility}")
                descriptor.annotations.forEach { annotation ->
                    println("  Annotation: ${annotation.name}")
                    annotation.parameters.forEach { (key, value) ->
                        println("    $key: $value")
                    }
                }
                descriptor.methods.forEach { method ->
                    println("  Method: ${method.name}")
                    println("    Visibility: ${method.visibility}")
                    method.annotations.forEach { annotation ->
                        println("    Annotation: ${annotation.name}")
                        annotation.parameters.forEach { (key, value) ->
                            println("      $key: $value")
                        }
                    }
                }
                descriptor.fields.forEach { field ->
                    println("  Field: ${field.name}")
                    println("    Visibility: ${field.visibility}")
                    field.annotations.forEach { annotation ->
                        println("    Annotation: ${annotation.name}")
                        annotation.parameters.forEach { (key, value) ->
                            println("      $key: $value")
                        }
                    }
                }
            }

        } else {
            println("classes.jar not found in AAR file")
        }
    } else {
        println("AAR file not found")
    }
}

@KotlinxBenchmarkPluginInternalApi
fun Project.generateAndroidExecFile() {
    // TODO: It should be android template project directory
    val androidTestDir = File("E:\\Android\\AndroidProjects\\kotlin-qualification-task\\composeApp\\src\\androidTest\\kotlin")
    if (!androidTestDir.exists()) {
        androidTestDir.mkdirs()
    }

    // TODO: It should be android template project build.gradle.kts file
    val buildGradleFile = File("E:\\Android\\AndroidProjects\\kotlin-qualification-task\\composeApp\\build.gradle.kts")
    val dependencyPaths = listOf(
        "${project.projectDir}\\build\\outputs\\unpacked-aar\\release\\classes.jar".replace("\\", "\\\\") to null,
        "androidx.benchmark:benchmark-junit4" to "1.2.4",
        "androidx.test.ext:junit-ktx" to "1.2.1",
        "junit:junit" to "4.13.2"
    )

    updateAndroidDependencies(buildGradleFile, dependencyPaths)

    val jarFile = JarFile(File("${project.projectDir}\\build\\outputs\\unpacked-aar\\release\\classes.jar"))
    val annotationProcessor = AnnotationProcessor()
    annotationProcessor.processJarFile(jarFile)
    jarFile.close()

    val classDescriptors = annotationProcessor.getClassDescriptors()
    val fileSpecBuilder = FileSpec.builder("generated", "GeneratedCode")
        .addImport("androidx.test.ext.junit.runners", "AndroidJUnit4")
        .addImport("org.junit", "Test")
        .addImport("org.junit.runner", "RunWith")
    val typeSpecBuilder = TypeSpec.classBuilder("GeneratedCode")
        .addAnnotation(
            AnnotationSpec.builder(ClassName("org.junit.runner", "RunWith"))
                .addMember("%T::class", ClassName("androidx.test.ext.junit.runners", "AndroidJUnit4"))
                .build()
        )

    val uniquePropertyNames = mutableSetOf<String>()

    classDescriptors.forEach { descriptor ->
        if (descriptor.visibility == Visibility.PUBLIC && !descriptor.isAbstract) {
            val simpleClassName = descriptor.name
            val fullyQualifiedName = "${descriptor.packageName}.$simpleClassName"

            var propertyName = "${descriptor.packageName.replace('.', '_')}_$simpleClassName".decapitalize()
            if (!propertyName.endsWith("Benchmark")) {
                propertyName += "Benchmark"
            }

            while (!uniquePropertyNames.add(propertyName)) {
                propertyName += "_"
            }

            val propertySpec = PropertySpec.builder(propertyName, ClassName.bestGuess(fullyQualifiedName))
                .initializer("%T()", ClassName.bestGuess(fullyQualifiedName))
                .build()
            typeSpecBuilder.addProperty(propertySpec)

            val methodSpecBuilder = FunSpec.builder("benchmarkExecutor_${descriptor.packageName.replace('.', '_')}_$simpleClassName")
                .addAnnotation(ClassName("org.junit", "Test"))
                .addCode(buildBenchmarkTestFunctionCode(propertyName, descriptor.methods))

            typeSpecBuilder.addFunction(methodSpecBuilder.build())
        }
    }

    fileSpecBuilder.addType(typeSpecBuilder.build())
    fileSpecBuilder.build().writeTo(androidTestDir)
}

private fun buildBenchmarkTestFunctionCode(propertyName: String, methods: List<MethodAnnotationsDescriptor>): CodeBlock {
    val builder = CodeBlock.builder()

    val setUpMethod = methods.find { it.name == "setUp" }
    val tearDownMethod = methods.find { it.name == "tearDown" }
    val benchmarkMethods = methods.filter { it.name != "setUp" && it.name != "tearDown" && it.visibility == Visibility.PUBLIC }

    setUpMethod?.let {
        builder.addStatement("$propertyName.${it.name}()")
    }

    benchmarkMethods.forEach { method ->
        builder.addStatement("$propertyName.${method.name}()")
    }

    tearDownMethod?.let {
        builder.addStatement("$propertyName.${it.name}()")
    }

    return builder.build()
}

private fun updateAndroidDependencies(buildGradleFile: File, dependencies: List<Pair<String, String?>>) {
    if (buildGradleFile.exists()) {
        val buildGradleContent = buildGradleFile.readText()

        // Check if the android block exists
        if (buildGradleContent.contains("android {")) {
            // Check if the dependencies block inside android contains the dependencyString
            val androidBlockStart = buildGradleContent.indexOf("android {")
            val androidBlockEnd = buildGradleContent.lastIndexOf("}") + 1
            val androidBlockContent = buildGradleContent.substring(androidBlockStart, androidBlockEnd)

            val newDependencies = dependencies.filterNot { (dependency, version) ->
                val dependencyString = version?.let { """$dependency:$version""" } ?: dependency
                androidBlockContent.contains(dependencyString)
            }
            if (newDependencies.isNotEmpty()) {
                // Add the dependencies inside the android { dependencies { ... } } block
                val updatedAndroidBlockContent = if (androidBlockContent.contains("dependencies {")) {
                    val dependenciesBlockStart = androidBlockContent.indexOf("dependencies {")
                    val dependenciesBlockEnd = androidBlockContent.indexOf("}", dependenciesBlockStart) + 1
                    val dependenciesBlockContent =
                        androidBlockContent.substring(dependenciesBlockStart, dependenciesBlockEnd)

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

                // Replace the old android block with the updated one
                val updatedBuildGradleContent =
                    buildGradleContent.replace(androidBlockContent, updatedAndroidBlockContent)
                buildGradleFile.writeText(updatedBuildGradleContent)
            }
        }
    }
}