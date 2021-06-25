import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.konan.target.*

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

evaluationDependsOn(":kotlinx-benchmark-runtime")

val Gradle.isConfigurationCacheAvailable
    get() = try {
        val startParameters = gradle.startParameter
        startParameters.javaClass.getMethod("isConfigurationCache")
            .invoke(startParameters) as? Boolean
    } catch (_: Exception) {
        null
    } ?: false

fun Project.getSystemProperty(key: String): String? {
    return if (gradle.isConfigurationCacheAvailable) {
        providers.systemProperty(key).forUseAtConfigurationTime().orNull
    } else {
        System.getProperty(key)
    }
}

val nativeTargetName
    get() = when {
        project.getSystemProperty("idea.active") == "true" -> "native"
        HostManager.hostIsLinux -> "linuxX64"
        HostManager.hostIsMingw -> "mingwX64"
        HostManager.hostIsMac -> "macosX64"
        else -> error("Unknown host: ${HostManager.host}")
    }

val runtime get() = project(":kotlinx-benchmark-runtime")
val plugin get() = gradle.includedBuild("plugin")

val AbstractArchiveTask.archiveFilePath get() = archiveFile.get().asFile.path

fun artifactsTask(artifact: String) = runtime.tasks.getByName<AbstractArchiveTask>("${artifact}Jar")
fun artifactsTaskNativeKlibs() = runtime.tasks.getByName("compileKotlin${nativeTargetName.capitalize()}")

fun Task.klibs(): String = outputs.files.filter { it.extension == "klib" }.joinToString("\n")

fun IncludedBuild.classpath() = projectDir.resolve("build/createClasspathManifest")

val createClasspathManifest by tasks.registering {
    dependsOn(plugin.task(":createClasspathManifest"))
    dependsOn(artifactsTask("jvm"))
    dependsOn(artifactsTask("js"))
    dependsOn(artifactsTask("metadata"))
    dependsOn(artifactsTaskNativeKlibs())

    val outputDir = file("$buildDir/$name")
    outputs.dir(outputDir)
    doLast {
        outputDir.apply {
            mkdirs()
            resolve("plugin-classpath.txt").writeText(plugin.classpath().resolve("plugin-classpath.txt").readText())
            resolve("runtime-metadata.txt").writeText(artifactsTask("metadata").archiveFilePath)
            resolve("runtime-jvm.txt").writeText(artifactsTask("jvm").archiveFilePath)
            resolve("runtime-js.txt").writeText(artifactsTask("js").archiveFilePath)
            resolve("runtime-native.txt").writeText(artifactsTaskNativeKlibs().klibs())
        }
    }
}

dependencies {
    implementation(files(createClasspathManifest))
    implementation(gradleTestKit())

    testImplementation(kotlin("test-junit"))
}
