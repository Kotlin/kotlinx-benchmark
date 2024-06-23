package kotlinx.benchmark.gradle

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
            val entries = jar.entries()

            val annotationProcessor = AnnotationProcessor()

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.endsWith(".class")) {
                    val inputStream = jar.getInputStream(entry)
                    val classBytes = inputStream.readBytes()
                    annotationProcessor.processClassBytes(classBytes)
                }
            }
            jar.close()

            val classAnnotationsDescriptors = annotationProcessor.getClassDescriptors()
            println("Class annotations for ${target.name}:")

            classAnnotationsDescriptors.forEach { descriptor ->
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