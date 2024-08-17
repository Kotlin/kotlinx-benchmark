package kotlinx.benchmark.gradle

import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import java.io.InputStream
import java.net.URLDecoder
import java.util.*
import java.util.concurrent.TimeUnit

private const val GENERATED_ANDROID_PROJECT_NAME = "GeneratedAndroidProject"

internal fun Project.processAndroidCompilation(target: AndroidBenchmarkTarget, compilation: KotlinJvmAndroidCompilation) {
    project.logger.info("Configuring benchmarks for '${compilation.name}' using $target")

    createUnpackAarTask(target, compilation)
    createSetupAndroidProjectTask(target, compilation)
    createAndroidBenchmarkGenerateSourceTask(target, compilation)
    createAndroidBenchmarkExecTask(target, compilation)
}

private fun Project.androidBenchmarkBuildDir(target: AndroidBenchmarkTarget, compilation: KotlinJvmAndroidCompilation) =
    benchmarkBuildDir(target).resolve(compilation.name)

private fun Project.generatedAndroidProjectDir(target: AndroidBenchmarkTarget, compilation: KotlinJvmAndroidCompilation) =
    androidBenchmarkBuildDir(target, compilation).resolve(GENERATED_ANDROID_PROJECT_NAME)

private fun Project.createSetupAndroidProjectTask(target: AndroidBenchmarkTarget, compilation: KotlinJvmAndroidCompilation) {
    task<DefaultTask>("setup${compilation.name.capitalize(Locale.ROOT)}AndroidProject") {
        group = "benchmark"
        description = "Sets up an empty android project to generate benchmarks into"

        doFirst {
            sync {
                it.apply {
                    val pluginJarPath = BenchmarksPlugin::class.java.protectionDomain.codeSource.location.path
                    from(project.zipTree(URLDecoder.decode(pluginJarPath, "UTF-8")))
                    into(androidBenchmarkBuildDir(target, compilation))
                    include("$GENERATED_ANDROID_PROJECT_NAME/**")
                }
            }
        }
        doLast {
            val generatedAndroidProjectDir = generatedAndroidProjectDir(target, compilation)
            logger.info("Setting up an empty Android project at $generatedAndroidProjectDir")

            generatedAndroidProjectDir.resolve("microbenchmark/build.gradle.kts").let {
                val unpackedDir = getUnpackAarDir(compilation)
                val newText = it.readText().replace(
                    "<<BENCHMARK_CLASSES_JAR_PATH>>",
                    unpackedDir.resolve("classes.jar").absolutePath.replace("\\", "/")
                )
                it.writeText(newText)
            }
        }
    }
}

private fun Project.createUnpackAarTask(target: AndroidBenchmarkTarget, compilation: KotlinJvmAndroidCompilation) {
    task<DefaultTask>("unpack${compilation.name.capitalize(Locale.ROOT)}Aar") {
        group = "benchmark"
        description = "Unpacks the AAR file produced by ${target.name} compilation '${compilation.name}'"
        dependsOn("bundle${compilation.name.capitalize(Locale.ROOT)}Aar")
        doLast {
            logger.info("Unpacking AAR file produced by ${target.name} compilation '${compilation.name}'")

            val aarFile = getAarFile(compilation)

            if (!aarFile.exists()) {
                throw IllegalStateException("AAR file not found: ${aarFile.absolutePath}")
            }

            // TODO: Register the unpacked dir as an output of this task
            // TODO: Delete the directory if exists before unpacking
            unpackAarFile(aarFile, compilation)
        }
    }
}

private fun generateSourcesTaskName(target: AndroidBenchmarkTarget, compilation: KotlinJvmAndroidCompilation): String {
    return "${target.name}${compilation.name.capitalize(Locale.ROOT)}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}"
}

private fun Project.createAndroidBenchmarkGenerateSourceTask(target: AndroidBenchmarkTarget, compilation: KotlinJvmAndroidCompilation) {
    task<DefaultTask>(generateSourcesTaskName(target, compilation)) {
        group = "benchmark"
        description = "Generates Android source files for ${target.name} compilation '${compilation.name}'"
        dependsOn("unpack${compilation.name.capitalize(Locale.ROOT)}Aar")
        dependsOn("setup${compilation.name.capitalize(Locale.ROOT)}AndroidProject")

        doLast {

            val unpackedDir = getUnpackAarDir(compilation)
            processClassesJar(unpackedDir, compilation) { classDescriptors ->
                val targetDir = generatedAndroidProjectDir(target, compilation)
                    .resolve("microbenchmark/src/androidTest/kotlin")

                targetDir.mkdirs()

                generateBenchmarkSourceFiles(targetDir, classDescriptors)
            }
        }
    }
}

private fun detectAndroidDevice() {
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
private fun Project.createAndroidBenchmarkExecTask(target: AndroidBenchmarkTarget, compilation: KotlinJvmAndroidCompilation) {
    task<DefaultTask>("android${compilation.name.capitalize(Locale.ROOT)}Benchmark") {
        group = "benchmark"
        description = "Executes benchmarks for ${target.name} compilation '${compilation.name}'"
        dependsOn(generateSourcesTaskName(target, compilation))
        doLast {
            detectAndroidDevice()

            // TODO: Project path needs to execute benchmark task
            val executeBenchmarkPath = generatedAndroidProjectDir(target, compilation).path
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

                val outputGobbler = StreamGobbler(process.inputStream) { line ->
                    if (line.contains("Iteration") || line.contains("run finished")) {
                        println(line)
                    }
                }

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

private class StreamGobbler(private val inputStream: InputStream, private val consumer: (String) -> Unit) : Thread() {
    override fun run() {
        inputStream.bufferedReader().useLines { lines ->
            lines.forEach { consumer(it) }
        }
    }
}