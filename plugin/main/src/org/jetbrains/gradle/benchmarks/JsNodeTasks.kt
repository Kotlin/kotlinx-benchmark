package org.jetbrains.gradle.benchmarks

import com.moowork.gradle.node.*
import com.moowork.gradle.node.npm.*
import com.moowork.gradle.node.task.*
import org.gradle.api.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.*

fun Project.createJsBenchmarkInstallTask() {
    val node = project.extensions.getByType(NodeExtension::class.java)
    task<NpmTask>("npmInstallBenchmarkJs") {
        group = "node"
        description = "Install benchmark.js to local node_modules"
        setArgs(listOf("install", "benchmark"))
        setWorkingDir(node.nodeModulesDir) // For some reason configured node_modules dir is not picked up
    }
}


fun Project.createJsBenchmarkExecTask(
    extension: BenchmarksExtension,
    config: BenchmarkConfiguration,
    compilation: KotlinJsCompilation
) {
    val node = project.extensions.getByType(NodeExtension::class.java)
    val nodeModulesDir = node.nodeModulesDir.resolve("node_modules")
    task<NodeTask>("${config.name}${BenchmarksPlugin.BENCHMARK_EXEC_SUFFIX}") {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Executes benchmark for '${config.name}'"
        //setScript(file("$nodeModulesDir/${compilation.output}"))
        val jsTask = tasks.getByName(compilation.compileKotlinTaskName) as Kotlin2JsCompile
        setScript(nodeModulesDir.resolve(jsTask.outputFile.name))
        setWorkingDir(nodeModulesDir)
        //args = [testCompilationTask.outputFile, '--require', 'source-map-support/register']
        dependsOn("${config.name}${BenchmarksPlugin.BENCHMARK_DEPENDENCIES_SUFFIX}")
        tasks.getByName("benchmark").dependsOn(this)
    }
}

fun Project.createJsBenchmarkDependenciesTask(
    extension: BenchmarksExtension,
    config: BenchmarkConfiguration,
    compilation: KotlinJsCompilation
) {
    val node = project.extensions.getByType(NodeExtension::class.java)
    val nodeModulesDir = node.nodeModulesDir.resolve("node_modules")
    task<Copy>("${config.name}${BenchmarksPlugin.BENCHMARK_DEPENDENCIES_SUFFIX}") {
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

}

