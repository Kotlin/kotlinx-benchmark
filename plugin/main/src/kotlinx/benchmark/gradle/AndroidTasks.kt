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
        } else {
            println("classes.jar not found in AAR file")
        }
    } else {
        println("AAR file not found")
    }
}