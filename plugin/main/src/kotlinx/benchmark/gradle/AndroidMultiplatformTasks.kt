package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import java.util.*


@KotlinxBenchmarkPluginInternalApi
fun Project.processAndroidCompilation(target: KotlinJvmAndroidCompilation) {
    project.logger.info("Configuring benchmarks for '${target.name}' using Kotlin/Android")
    println("processAndroidCompilation: ${target.name}")
    val compilation = target.target.compilations.names.let(::println)

    tasks.register("process${target.name.capitalize(Locale.getDefault())}Compilation", DefaultTask::class.java) {
        it.group = "benchmark"
        it.description = "Processes the Android compilation '${target.name}' for benchmarks"
        it.dependsOn("bundle${target.name.capitalize(Locale.getDefault())}Aar")
        it.doLast {
            unpackAndProcessAar(target)
            //generateAndroidExecFile()
            detectAndroidDevice()
        }
    }
}

fun Project.detectAndroidDevice() {
    val devices = ProcessBuilder("adb", "devices")
        .start()
        .inputStream
        .bufferedReader()
        .useLines { lines ->
            lines.filter { it.endsWith("device") }
                .map { it.substringBefore("\t") }
                .toList()
        }
    devices.takeIf { it.isNotEmpty() }
        ?.let {
            println("Connected Android devices/emulators:\n\t${it.joinToString("\n\t")}")
        } ?: throw RuntimeException("No Android devices/emulators found, please start an emulator or connect a device.")
}