package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import java.io.File

@KotlinxBenchmarkPluginInternalApi
fun Project.unpackAndProcessAar(target: KotlinJvmAndroidCompilation) {
    println("Unpacking AAR file for ${target.name}")
    val aarFile = File("${project.projectDir}/build/outputs/aar/${project.name}-${target.name}.aar")
    if (aarFile.exists()) {
        val unpackedDir = File("${project.projectDir}/build/outputs/unpacked-aar/${target.name}")
        val classesJar = File(unpackedDir, "classes.jar")
        val unzipDir = File("${project.projectDir}/build/outputs/unzipped-classes/${target.name}")

        // Unpack AAR file
        project.copy {
            it.from(project.zipTree(aarFile))
            it.into(unpackedDir)
        }

        if (classesJar.exists()) {
            project.copy {
                it.from(project.zipTree(classesJar))
                it.into(unzipDir)
            }
            println("Unzipped classes.jar to: $unzipDir")

            // Process the .class files to retrieve annotation data
            val annotationProcessor = AnnotationProcessor()
            unzipDir.walk().forEach { file ->
                if (file.extension == "class") {
                    println("Processing class file: $file")
                    annotationProcessor.processClassFile(file)
                }
            }

            val annotations = annotationProcessor.getClassAnnotations()
            annotations.forEach { (className, classAnnotations) ->
                println("Annotation for class: $className")

                classAnnotations.classAnnotations.forEach { (annotationDesc, annotationData) ->
                    println("Class annotation: $annotationDesc")
                    annotationData.parameters.forEach { (name, value) ->
                        println("  - $name: $value")
                    }
                }

                classAnnotations.methodAnnotations.forEach { (methodName, methodAnnotationMap) ->
                    methodAnnotationMap.forEach { (annotationDesc, annotationData) ->
                        println("Method annotation in $methodName: $annotationDesc")
                        annotationData.parameters.forEach { (name, value) ->
                            println("  - $name: $value")
                        }
                    }
                }

                classAnnotations.fieldAnnotations.forEach { (fieldName, fieldAnnotationMap) ->
                    fieldAnnotationMap.forEach { (annotationDesc, annotationData) ->
                        println("Field annotation in $fieldName: $annotationDesc")
                        annotationData.parameters.forEach { (name, value) ->
                            println("  - $name: $value")
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