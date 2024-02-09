package kotlinx.benchmark.gradle

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.toolchain.JavaCompiler
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import java.io.File
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun cleanup(file: File) {
    if (file.exists()) {
        val listing = file.listFiles()
        if (listing != null) {
            for (sub in listing) {
                cleanup(sub)
            }
        }
        file.delete()
    }
}

inline fun <reified T : Task> Project.task(
    name: String,
    depends: String? = null,
    noinline configuration: T.() -> Unit
): TaskProvider<T> {
    @Suppress("UnstableApiUsage")
    val task = tasks.register(name, T::class.java, Action(configuration))
    if (depends != null) {
        tasks.getByName(depends).dependsOn(task)
    }
    return task
}

fun Project.benchmarkBuildDir(target: BenchmarkTarget): File =
    file(buildDir.resolve(target.extension.buildDir).resolve(target.name))

fun Project.benchmarkReportsDir(config: BenchmarkConfiguration, target: BenchmarkTarget): File {
    val ext = project.extensions.extraProperties
    val time = if (ext.has("reportTime")) {
        ext.get("reportTime") as LocalDateTime
    } else {
        LocalDateTime.now().also {
            ext.set("reportTime", it)
        }
    }
    val timestamp = time.format(DateTimeFormatter.ISO_DATE_TIME)
    val compatibleTime = timestamp.replace(":", ".") // Windows doesn't allow ':' in path
    return file(buildDir.resolve(target.extension.reportsDir).resolve(config.name).resolve(compatibleTime))
}

class KotlinClosure1<in T : Any?, V : Any>(
    val function: T.() -> V?,
    owner: Any? = null,
    thisObject: Any? = null
) : Closure<V?>(owner, thisObject) {

    @Suppress("unused") // to be called dynamically by Groovy
    fun doCall(it: T): V? = it.function()
}

fun <T> Any.closureOf(action: T.() -> Unit): Closure<Any?> =
    KotlinClosure1(action, this, this)

fun <T> Any.tryGetClass(className: String): Class<T>? {
    val classLoader = javaClass.classLoader
    return try {
        @Suppress("UNCHECKED_CAST")
        Class.forName(className, false, classLoader) as Class<T>
    } catch (e: ClassNotFoundException) {
        null
    }
}

fun Task.setupReporting(target: BenchmarkTarget, config: BenchmarkConfiguration): File {
    extensions.extraProperties.set("idea.internal.test", project.getSystemProperty("idea.active"))
    val reportsDir = project.benchmarkReportsDir(config, target)
    val reportFile = reportsDir.resolve("${target.name}.${config.reportFileExt()}")
    val configName = config.name
    val targetName = target.name
    doFirst {
        reportsDir.mkdirs()
        logger.lifecycle("Running '${configName}' benchmarks for '${targetName}'")
    }
    return reportFile
}

fun Task.traceFormat(): String {
    val ideaActive = project.getSystemProperty("idea.active").toBoolean()
    return if (ideaActive) "xml" else "text"
}

val Path.absolutePath: String get() = toAbsolutePath().toFile().invariantSeparatorsPath

fun writeParameters(
    name: String,
    reportFile: File,
    format: String,
    config: BenchmarkConfiguration
): File {
    validateConfig(config)
    val file = createTempFile("benchmarks")
    file.writeText(buildString {
        appendLine("name:$name")
        appendLine("reportFile:$reportFile")
        appendLine("traceFormat:$format")
        config.reportFormat?.let { appendLine("reportFormat:$it") }
        config.iterations?.let { appendLine("iterations:$it") }
        config.warmups?.let { appendLine("warmups:$it") }
        config.iterationTime?.let { appendLine("iterationTime:$it") }
        config.iterationTimeUnit?.let { appendLine("iterationTimeUnit:$it") }
        config.outputTimeUnit?.let { appendLine("outputTimeUnit:$it") }
        config.mode?.let { appendLine("mode:$it") }

        config.includes.forEach {
            appendLine("include:$it")
        }
        config.excludes.forEach {
            appendLine("exclude:$it")
        }
        config.params.forEach { (param, values) ->
            values.forEach { value -> appendLine("param:$param=$value") }
        }
        config.advanced.forEach { (param, value) ->
            appendLine("advanced:$param=$value")
        }
    })
    return file
}

private fun validateConfig(config: BenchmarkConfiguration) {
    config.reportFormat?.let {
        require(it.toLowerCase() in ValidOptions.format) {
            "Invalid report format: '$it'. Accepted formats: ${ValidOptions.format.joinToString(", ")} (e.g., reportFormat = \"json\")."
        }
    }

    config.iterations?.let {
        require(it > 0) {
            "Invalid iterations: '$it'. Expected a positive integer (e.g., iterations = 5)."
        }
    }

    config.warmups?.let {
        require(it >= 0) {
            "Invalid warmups: '$it'. Expected a non-negative integer (e.g., warmups = 3)."
        }
    }

    config.iterationTime?.let {
        require(it > 0) {
            "Invalid iterationTime: '$it'. Expected a positive number (e.g., iterationTime = 300)."
        }
        require(config.iterationTimeUnit != null) {
            "Missing iterationTimeUnit. Please provide iterationTimeUnit when specifying iterationTime."
        }
    }

    config.iterationTimeUnit?.let {
        require(it in ValidOptions.timeUnits) {
            "Invalid iterationTimeUnit: '$it'. Accepted units: ${ValidOptions.timeUnits.joinToString(", ")} (e.g., iterationTimeUnit = \"ms\")."
        }
        require(config.iterationTime != null) {
            "Missing iterationTime. Please provide iterationTime when specifying iterationTimeUnit."
        }
    }

    config.mode?.let {
        require(it in ValidOptions.modes) {
            "Invalid benchmark mode: '$it'. Accepted modes: ${ValidOptions.modes.joinToString(", ")} (e.g., mode = \"thrpt\")."
        }
    }
    
    config.outputTimeUnit?.let {
        require(it in ValidOptions.timeUnits) {
            "Invalid outputTimeUnit: '$it'. Accepted units: ${ValidOptions.timeUnits.joinToString(", ")} (e.g., outputTimeUnit = \"ns\")."
        }
    }    

    config.includes.forEach { pattern ->
        require(pattern.isNotBlank()) {
            "Invalid include pattern: '$pattern'. Pattern must not be blank."
        }
    }

    config.excludes.forEach { pattern ->
        require(pattern.isNotBlank()) {
            "Invalid exclude pattern: '$pattern'. Pattern must not be blank."
        }
    }

    config.params.forEach { (param, values) ->
        require(param.isNotBlank()) {
            "Invalid param name: '$param'. It must not be blank."
        }
        require(values.isNotEmpty()) {
            "Param '$param' has no values. At least one value is required."
        }
    }

    config.advanced.forEach { (param, value) ->
        require(param.isNotBlank()) {
            "Invalid advanced option name: '$param'. It must not be blank."
        }
        require(value.toString().isNotBlank()) {
            "Invalid value for advanced option '$param': '$value'. Value should not be blank."
        }

        when (param) {
            "nativeFork" -> {
                require(value.toString() in ValidOptions.nativeForks) {
                    "Invalid value for 'nativeFork': '$value'. Accepted values: ${ValidOptions.nativeForks.joinToString(", ")}."
                }
            }
            "nativeGCAfterIteration" -> require(value is Boolean) {
                "Invalid value for 'nativeGCAfterIteration': '$value'. Expected a Boolean value."
            }
            "jvmForks" -> {
                val intValue = value.toString().toIntOrNull()
                require(intValue != null && intValue >= 0 || value.toString() == "definedByJmh") {
                    "Invalid value for 'jvmForks': '$value'. Expected a non-negative integer or \"definedByJmh\"."
                }
            }
            "jsUseBridge" -> require(value is Boolean) {
                "Invalid value for 'jsUseBridge': '$value'. Expected a Boolean value."
            }
            else -> throw IllegalArgumentException("Invalid advanced option name: '$param'. Accepted options: \"nativeFork\", \"nativeGCAfterIteration\", \"jvmForks\", \"jsUseBridge\".")
        }
    }
}

private object ValidOptions {
    val format = setOf("json", "csv", "scsv", "text")
    val timeUnits = setOf(
        "NANOSECONDS", "ns", "nanos",
        "MICROSECONDS", "us", "micros",
        "MILLISECONDS", "ms", "millis",
        "SECONDS", "s", "sec",
        "MINUTES", "m", "min"
    )
    val modes = setOf("thrpt", "avgt", "Throughput", "AverageTime")
    val nativeForks = setOf("perBenchmark", "perIteration")
}

internal fun Project.getSystemProperty(key: String): String? {
    return providers.systemProperty(key).orNull
}

fun Project.javaCompilerProvider(): Provider<JavaCompiler> = provider {
    val toolchainService = extensions.findByType(JavaToolchainService::class.java) ?: return@provider null
    val javaExtension = extensions.findByType(JavaPluginExtension::class.java) ?: return@provider null
    toolchainService.compilerFor(javaExtension.toolchain).orNull
}

fun Project.javaLauncherProvider(): Provider<JavaLauncher> = provider {
    val toolchainService = extensions.findByType(JavaToolchainService::class.java) ?: return@provider null
    val javaExtension = extensions.findByType(JavaPluginExtension::class.java) ?: return@provider null
    toolchainService.launcherFor(javaExtension.toolchain).orNull
}
