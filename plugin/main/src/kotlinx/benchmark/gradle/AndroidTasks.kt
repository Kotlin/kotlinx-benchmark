package kotlinx.benchmark.gradle

import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import java.io.File
import java.util.jar.JarFile

internal fun Project.getAarFile(compilation: KotlinJvmAndroidCompilation): File {
    return File("${project.projectDir}/build/outputs/aar/${project.name}-${compilation.name}.aar")
}

internal fun Project.getUnpackAarDir(compilation: KotlinJvmAndroidCompilation): File {
    return File("${project.projectDir}/build/outputs/unpacked-aar/${compilation.name}")
}

internal fun Project.unpackAarFile(aarFile: File, compilation: KotlinJvmAndroidCompilation): File {
    val unpackedDir = getUnpackAarDir(compilation)
    project.sync {
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

internal fun processClassesJar(
    unpackedDir: File,
    compilation: KotlinJvmAndroidCompilation,
    onProcessed: (List<ClassAnnotationsDescriptor>) -> Unit) {
    val classesJar = File(unpackedDir, "classes.jar")
    if (classesJar.exists()) {
        println("Processing classes.jar for ${compilation.name}")
        val jar = JarFile(classesJar)
        val annotationProcessor = AnnotationProcessor()
        annotationProcessor.processJarFile(jar)
        jar.close()
        onProcessed(annotationProcessor.getClassDescriptors())
    } else {
        println("classes.jar not found in AAR file")
    }
}