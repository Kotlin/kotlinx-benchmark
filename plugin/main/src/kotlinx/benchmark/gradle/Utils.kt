package kotlinx.benchmark.gradle

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskProvider
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
        require(it.toLowerCase() in setOf("json", "csv", "scsv", "text")) {
            "Report format '$it' is not supported."
        }
    }

    config.iterations?.let {
        require(it > 0) {
            "Iterations must be greater than 0."
        }
    }

    config.warmups?.let {
        require(it >= 0) {
            "Warmups must be equal to or greater than 0."
        }
    }

    config.iterationTime?.let {
        require(it > 0) {
            "Iteration time must be greater than 0."
        }
        require(config.iterationTimeUnit != null) {
            "If iterationTime is provided, iterationTimeUnit must also be provided."
        }
    }
    
    config.iterationTimeUnit?.let {
        require(it.toLowerCase() in setOf(
            "seconds", "s", "microseconds", "us", "milliseconds", "ms",
            "nanoseconds", "ns", "minutes", "m"
        )) {
            "Unknown time unit: $it"
        }
        require(config.iterationTime != null) {
            "If iterationTimeUnit is provided, iterationTime must also be provided."
        }
    }
    

    config.mode?.let {
        require(it.toLowerCase() in setOf("thrpt", "avgt")) {
            "Benchmark mode '$it' is not supported."
        }
    }

    config.outputTimeUnit?.let {
        require(it.toLowerCase() in setOf(
            "seconds", "s", "microseconds", "us", "milliseconds", "ms",
            "nanoseconds", "ns", "minutes", "m"
        )) {
            "Unknown time unit: $it"
        }
    }

    config.includes.forEach {
        require(it.isNotBlank()) {
            "Include pattern should not be blank."
        }
    }

    // Validate exclude
    config.excludes.forEach {
        require(it.isNotBlank()) {
            "Exclude pattern should not be blank."
        }
    }

    // Validate params
    config.params.forEach { (param, values) ->
        require(param.isNotBlank()) {
            "Param name should not be blank."
        }
        require(values.isNotEmpty()) {
            "Param '$param' should have at least one value."
        }
    }

    // Validate advanced
    config.advanced.forEach { (param, value) ->
        println("Validating advanced param: $param with value: $value")
        require(param.isNotBlank()) {
            "Advanced config name should not be blank."
        }
        require(value.toString().isNotBlank()) {
            "Value for advanced config '$param' should not be blank."
        }

        // Specific advanced config validations
        when (param) {
            "nativeFork" -> require(value.toString().toLowerCase() in setOf("perbenchmark", "periteration")) {
                "Invalid value '$value' for 'nativeFork'. It should be either 'perBenchmark' or 'perIteration'."
            }
            "nativeGCAfterIteration" -> require(value is Boolean) {
                "Invalid value '$value' for 'nativeGCAfterIteration'. It should be a Boolean value."
            }
            "jvmForks" -> {
                val intValue = value.toString().toIntOrNull()
                require(intValue != null && intValue >= 0 || value.toString().toLowerCase() == "definedbyjmh") {
                    "Invalid value '$value' for 'jvmForks'. It should be a non-negative integer, or 'definedByJmh'."
                }
            }
            "jsUseBridge" -> require(value is Boolean) {
                "Invalid value '$value' for 'jsUseBridge'. It should be a Boolean value."
            }
            else -> throw IllegalArgumentException("Invalid advanced config parameter '$param'. Allowed parameters are 'nativeFork', 'nativeGCAfterIteration', 'jvmForks', and 'jsUseBridge'.")
        }
    }
}

internal val Gradle.isConfigurationCacheAvailable
    get() = try {
        val startParameters = gradle.startParameter
        startParameters.javaClass.getMethod("isConfigurationCache")
            .invoke(startParameters) as? Boolean
    } catch (_: Exception) {
        null
    } ?: false

internal fun Project.getSystemProperty(key: String): String? {
    return if (gradle.isConfigurationCacheAvailable) {
        providers.systemProperty(key).forUseAtConfigurationTime().orNull
    } else {
        System.getProperty(key)
    }
}