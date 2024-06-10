package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.tree.*
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
        } else {
            println("classes.jar not found in AAR file")
        }
    } else {
        println("AAR file not found")
    }
}

class AnnotationProcessor {

    fun processClassFile(classFile: File) {
        val classReader = ClassReader(classFile.readBytes())
        val classNode = ClassNode()
        classReader.accept(classNode, 0)

        // Retrieve annotations from the class
        classNode.visibleAnnotations?.forEach { annotationNode ->
            println("Class annotation: ${annotationNode.desc}")
        }

        // Retrieve annotations from the methods
        classNode.methods?.forEach { methodNode ->
            methodNode.visibleAnnotations?.forEach { annotationNode ->
                println("Method annotation in ${methodNode.name}: ${annotationNode.desc}")
            }
        }

        // Retrieve annotations from the fields
        classNode.fields?.forEach { fieldNode ->
            fieldNode.visibleAnnotations?.forEach { annotationNode ->
                println("Field annotation in ${fieldNode.name}: ${annotationNode.desc}")
            }
        }
    }
}