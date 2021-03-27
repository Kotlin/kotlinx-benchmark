package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.BenchmarksPlugin.Companion.RUN_BENCHMARKS_TASKNAME
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.dsl.*
import org.jetbrains.kotlin.gradle.targets.js.ir.*
import org.jetbrains.kotlin.gradle.targets.js.nodejs.*

fun Project.createJsBenchmarkExecTask(
    config: BenchmarkConfiguration,
    target: JsBenchmarkTarget,
    compilation: KotlinJsCompilation
) {
    val taskName = "${target.name}${config.capitalizedName()}${BenchmarksPlugin.BENCHMARK_EXEC_SUFFIX}"
    val task = NodeJsExec.create(compilation, taskName) {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Executes benchmark for '${target.name}'"
        extensions.extraProperties.set("idea.internal.test", System.getProperty("idea.active"))
        nodeArgs.addAll(listOf("-r", "source-map-support/register"))

        val executableFile = when (val kotlinTarget = compilation.target) {
            is KotlinJsIrTarget -> {
                val binary = kotlinTarget.binaries.executable(compilation).first { it.mode == KotlinJsBinaryMode.PRODUCTION } as JsIrBinary
                binary.linkSyncTask.map {
                    it.destinationDir.resolve(binary.linkTask.get().outputFile.name)
                }
            }
            else -> compilation.compileKotlinTaskProvider.map { it.outputFile }
        }

        inputFileProperty.set(project.layout.file(executableFile))

        doFirst {
            val reportsDir = benchmarkReportsDir(config, target)
            val reportFile = reportsDir.resolve("${target.name}.${config.reportFileExt()}")
            val ideaActive = (extensions.extraProperties.get("idea.internal.test") as? String)?.toBoolean() ?: false
            args(writeParameters(target.name, reportFile, if (ideaActive) "xml" else "text", config))
            reportsDir.mkdirs()
            logger.lifecycle("Running '${config.name}' benchmarks for '${target.name}'")
        }
    }

    tasks.getByName(config.prefixName(RUN_BENCHMARKS_TASKNAME)).dependsOn(task)
}


