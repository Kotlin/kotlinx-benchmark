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
    doFirst {
        reportsDir.mkdirs()
        logger.lifecycle("Running '${config.name}' benchmarks for '${target.name}'")
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