package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.BenchmarksPlugin.Companion.RUN_BENCHMARKS_TASKNAME
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.nodejs.*
import org.jetbrains.kotlin.gradle.targets.js.npm.*

fun Project.createJsBenchmarkExecTask(
    config: BenchmarkConfiguration,
    target: JsBenchmarkTarget,
    compilation: KotlinJsCompilation
) {
    task<JsBenchmarkExec>(
        "${target.name}${config.capitalizedName()}${BenchmarksPlugin.BENCHMARK_EXEC_SUFFIX}",
        depends = config.prefixName(RUN_BENCHMARKS_TASKNAME)
    ) {
        dependsOn(compilation.runtimeDependencyFiles)

        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Executes benchmark for '${target.name}'"
        extensions.extraProperties.set("idea.internal.test", System.getProperty("idea.active"))

        val reportsDir = benchmarkReportsDir(config, target)
        val reportFile = reportsDir.resolve("${target.name}.json")

        val executableFile = compilation.compileKotlinTask.outputFile
        args("-r", "source-map-support/register")
        args(executableFile.absolutePath)
        workingDir = compilation.npmProject.dir

        onlyIf { executableFile.exists() }

        args("-n", target.name)
        args("-r", reportFile.toString())
        config.iterations?.let { args("-i", it.toString()) }
        config.warmups?.let { args("-w", it.toString()) }
        config.iterationTime?.let { args("-it", it.toString()) }
        config.iterationTimeUnit?.let { args("-itu", it) }
        config.outputTimeUnit?.let { args("-otu", it) }
        config.mode?.let { args("-m", it) }

        config.includes.forEach {
            args("-I", it)
        }
        config.excludes.forEach {
            args("-E", it)
        }
        config.params.forEach { (param, values) ->
            values.forEach { value -> args("-P", "\"$param=$value\"") }
        }

        doFirst {
            val ideaActive = (extensions.extraProperties.get("idea.internal.test") as? String)?.toBoolean() ?: false
            args("-t", if (ideaActive) "xml" else "text")
            reportsDir.mkdirs()
            logger.lifecycle("Running '${config.name}' benchmarks for '${target.name}'")
        }
    }
}

open class JsBenchmarkExec : NodeJsExec() {
/*
    @Option(option = "filter", description = "Configures the filter for benchmarks to run.")
    var filter: String? = null
*/
}


