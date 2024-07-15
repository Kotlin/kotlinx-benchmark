package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import java.io.File
import java.util.jar.JarFile

@KotlinxBenchmarkPluginInternalApi
fun Project.unpackAndProcessAar(
    target: KotlinJvmAndroidCompilation,
    onProcessed: (List<ClassAnnotationsDescriptor>) -> Unit
) {
    println("Unpacking AAR file for ${target.name}")
    val aarFile = getAarFile(target)
    if (aarFile.exists()) {
        val unpackedDir = unpackAarFile(aarFile, target)
        processClassesJar(unpackedDir, target, onProcessed)
    } else {
        println("AAR file not found")
    }
}

private fun Project.getAarFile(target: KotlinJvmAndroidCompilation): File {
    return File("${project.projectDir}/build/outputs/aar/${project.name}-${target.name}.aar")
}

private fun Project.unpackAarFile(aarFile: File, target: KotlinJvmAndroidCompilation): File {
    val unpackedDir = File("${project.projectDir}/build/outputs/unpacked-aar/${target.name}")
    project.copy {
        it.from(project.zipTree(aarFile))
        it.into(unpackedDir)
    }
    // unpack classes.jar
    val classesJar = File(unpackedDir, "classes.jar")
    if (classesJar.exists()) {
        val unpackedClassesDir = File(unpackedDir, "classes")
        project.copy {
            it.from(project.zipTree(classesJar))
            it.into(unpackedClassesDir)
        }
    }
    return unpackedDir
}

private fun Project.processClassesJar(
    unpackedDir: File,
    target: KotlinJvmAndroidCompilation,
    onProcessed: (List<ClassAnnotationsDescriptor>) -> Unit) {
    val classesJar = File(unpackedDir, "classes.jar")
    if (classesJar.exists()) {
        println("Processing classes.jar for ${target.name}")
        val jar = JarFile(classesJar)
        val annotationProcessor = AnnotationProcessor()
        annotationProcessor.processJarFile(jar)
        jar.close()
        onProcessed(annotationProcessor.getClassDescriptors())
    } else {
        println("classes.jar not found in AAR file")
    }
}