package org.jetbrains.gradle.benchmarks

import com.moowork.gradle.node.*
import com.moowork.gradle.node.npm.*
import com.moowork.gradle.node.task.*
import org.gradle.api.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

fun Project.createJsBenchmarkInstallTask() {
    val node = project.extensions.getByType(NodeExtension::class.java)
    task<NpmTask>("npmInstallBenchmarkJs") {
        group = "node"
        description = "Install benchmark.js to local node_modules"
        setArgs(listOf("install", "--prefer-offline", "benchmark", "-s"))
        setWorkingDir(node.nodeModulesDir) // For some reason configured node_modules dir is not picked up
    }
}


fun Project.createJsBenchmarkExecTask(
    config: BenchmarkConfiguration,
    compilation: KotlinJsCompilation
) {
    val node = project.extensions.getByType(NodeExtension::class.java)
    val nodeModulesDir = node.nodeModulesDir.resolve("node_modules")
    task<NodeTask>(
        "${config.name}${BenchmarksPlugin.BENCHMARK_EXEC_SUFFIX}",
        depends = BenchmarksPlugin.RUN_BENCHMARKS_TASKNAME
    ) {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Executes benchmark for '${config.name}'"
        extensions.extraProperties.set("idea.internal.test", System.getProperty("idea.active"))

        val reportsDir = benchmarkReportsDir(config)
        val reportFile = reportsDir.resolve("${config.name}.json")

        setScript(nodeModulesDir.resolve(compilation.compileKotlinTask.outputFile.name))
        addArgs(reportFile.toString(), config.iterations(), config.iterationTime()) // TODO: configure!
        setWorkingDir(nodeModulesDir)
        dependsOn("${config.name}${BenchmarksPlugin.BENCHMARK_DEPENDENCIES_SUFFIX}")
        doFirst {
            val ideaActive = (extensions.extraProperties.get("idea.internal.test") as? String)?.toBoolean() ?: false
            addArgs(if (ideaActive) "xml" else "text")
            reportsDir.mkdirs()
            logger.lifecycle("Running benchmarks for ${config.name}")
            logger.info("    I:${config.iterations()} T:${config.iterationTime()}")
        }
    }
}

fun Project.createJsBenchmarkDependenciesTask(
    config: BenchmarkConfiguration,
    compilation: KotlinJsCompilation
) {
    val node = project.extensions.getByType(NodeExtension::class.java)
    val nodeModulesDir = node.nodeModulesDir.resolve("node_modules")
    val deployTask = task<Copy>("${config.name}${BenchmarksPlugin.BENCHMARK_DEPENDENCIES_SUFFIX}") {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Copy dependencies of benchmark for '${config.name}'"
        val configuration = compilation.runtimeDependencyFiles
        val dependencies = configuration.files.map {
            if (it.name.endsWith(".jar")) {
                zipTree(it.absolutePath).matching {
                    include("*.js")
                    include("*.js.map")
                }
            } else {
                files(it)
            }
        }

        val dependencyFiles = files(dependencies).builtBy(configuration)
        from(compilation.output)
        from(dependencyFiles)
        into(nodeModulesDir)
        dependsOn("npmInstallBenchmarkJs")
        dependsOn(compilation.compileKotlinTaskName)
    }
    tasks.getByName(BenchmarksPlugin.ASSEMBLE_BENCHMARKS_TASKNAME).dependsOn(deployTask)
}

