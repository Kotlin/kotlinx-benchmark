package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.*
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit


@KotlinxBenchmarkPluginInternalApi
fun Project.processAndroidCompilation(target: KotlinJvmAndroidCompilation) {
    project.logger.info("Configuring benchmarks for '${target.name}' using Kotlin/Android")
    println("processAndroidCompilation: ${target.name}")
    val compilation = target.target.compilations.names.let(::println)

    val generateSourcesTask = tasks.register("processAndroid${target.name.capitalize(Locale.getDefault())}Compilation", DefaultTask::class.java) {
        it.group = "benchmark"
        it.description = "Processes the Android compilation '${target.name}' for benchmarks"
        it.dependsOn("bundle${target.name.capitalize(Locale.getDefault())}Aar")
        it.doLast {
            unpackAndProcessAar(target) { classDescriptors ->
                generateBenchmarkSourceFiles(classDescriptors)
            }
        }
    }

    createAndroidBenchmarkExecTask(target, generateSourcesTask)
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
fun Project.createAndroidBenchmarkExecTask(target: KotlinJvmAndroidCompilation, generateSourcesTask: TaskProvider<*>) {
    tasks.register("android${target.name.capitalize(Locale.getDefault())}Benchmark", DefaultTask::class.java) {
        it.group = "benchmark"
        it.description = "Processes the Android compilation '${target.name}' for benchmarks"
        it.dependsOn(generateSourcesTask)
        it.doLast {
            detectAndroidDevice()

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

                clearLogcat()
                val exitCode = process.waitFor(10, TimeUnit.MINUTES)
                captureLogcatOutput()
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
    }
}

private fun captureLogcatOutput() {
    try {
        val logcatProcess = ProcessBuilder("adb", "logcat", "-v", "time")
            .redirectErrorStream(true)
            .start()

        val logcatGobbler = StreamGobbler(logcatProcess.inputStream) { line ->
            when {
                line.contains("Iteration") -> println(line.substring(line.indexOf("Iteration")))
                line.contains("run finished") -> println(line.substring(line.indexOf("run finished")))
            }
        }

        logcatGobbler.start()

        if (!logcatProcess.waitFor(10, TimeUnit.SECONDS)) {
            logcatProcess.destroy()
        }

        logcatGobbler.join()
    } catch (e: Exception) {
        e.printStackTrace()
        throw GradleException("Failed to capture logcat output", e)
    }
}

private fun clearLogcat() {
    try {
        ProcessBuilder("adb", "logcat", "-c")
            .redirectErrorStream(true)
            .start()
            .waitFor(5, TimeUnit.SECONDS)
    } catch (e: Exception) {
        e.printStackTrace()
        throw GradleException("Failed to clear logcat", e)
    }
}

class StreamGobbler(
    private val inputStream: InputStream,
    private val consumer: (String) -> Unit
) : Thread() {
    override fun run() {
        inputStream.bufferedReader().useLines { lines ->
            lines.forEach { consumer(it) }
        }
    }
}