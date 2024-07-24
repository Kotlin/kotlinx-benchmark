package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit


@KotlinxBenchmarkPluginInternalApi
fun Project.processAndroidCompilation(target: KotlinJvmAndroidCompilation) {
    project.logger.info("Configuring benchmarks for '${target.name}' using Kotlin/Android")
    println("processAndroidCompilation: ${target.name}")
    val compilation = target.target.compilations.names.let(::println)

    tasks.register("processAndroid${target.name.capitalize(Locale.getDefault())}Compilation", DefaultTask::class.java) {
        it.group = "benchmark"
        it.description = "Processes the Android compilation '${target.name}' for benchmarks"
        it.dependsOn("bundle${target.name.capitalize(Locale.getDefault())}Aar")
        it.doLast {
            unpackAndProcessAar(target) { classDescriptors ->
                generateBenchmarkSourceFiles(classDescriptors)
            }
            detectAndroidDevice()
            createAndroidBenchmarkExecTask()
        }
    }
}

fun Project.detectAndroidDevice() {
    println("Detect running Android devices...")
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


// Use shell command to execute separate project gradle task
fun Project.createAndroidBenchmarkExecTask() {
    // TODO: Project path needs to execute benchmark task
    val executeBenchmarkPath = "E:/Android/AndroidProjects/kotlin-qualification-task"
    // Using ./gradlew on Windows shows error:
    // CreateProcess error=193, %1 is not a valid Win32 application
    val osName = System.getProperty("os.name").toLowerCase(Locale.ROOT)
    val gradlewPath = "$executeBenchmarkPath/gradlew" + if (osName.contains("win")) ".bat" else ""
    val args = listOf("-p", executeBenchmarkPath, "connectedAndroidTest")

    try {
        println("Running command: $gradlewPath ${args.joinToString(" ")}")

        val process = ProcessBuilder(gradlewPath, *args.toTypedArray())
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        val outputGobbler = StreamGobbler(process.inputStream) { println(it) }
        val errorGobbler = StreamGobbler(process.errorStream) { System.err.println(it) }

        outputGobbler.start()
        errorGobbler.start()

        val exitCode = process.waitFor(10, TimeUnit.MINUTES)
        if (!exitCode || process.exitValue() != 0) {
            println("Android benchmark task failed with exit code ${process.exitValue()}")
        } else {
            println("Benchmark for Android target finished.")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        throw GradleException("Failed to execute benchmark task", e)
    }
}

class StreamGobbler(private val inputStream: InputStream, private val consumer: (String) -> Unit) : Thread() {
    override fun run() {
        inputStream.bufferedReader().lines().forEach(consumer)
    }
}