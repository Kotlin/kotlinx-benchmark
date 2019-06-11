package kotlinx.benchmark.gradle

import kotlinx.team.infra.node.*
import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.component.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import kotlinx.benchmark.gradle.BenchmarksPlugin.Companion.RUN_BENCHMARKS_TASKNAME
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.*
import java.util.concurrent.*

fun Project.createJsBenchmarkInstallTask() {
    task<NpmInstallTask>("npmInstallBenchmarkJs") {
        group = "node"
        description = "Install benchmark.js to local node_modules"
        packages.add("benchmark")
        packages.add("source-map-support")
        dependsOn(NodeSetupTask.NAME)
    }
}

fun Project.createJsBenchmarkExecTask(
    config: BenchmarkConfiguration,
    target: JsBenchmarkTarget,
    compilation: KotlinJsCompilation
) {
    val node = NodeExtension[this]
    val nodeModulesDir = node.node_modules
    task<JsBenchmarkExec>(
        "${target.name}${config.capitalizedName()}${BenchmarksPlugin.BENCHMARK_EXEC_SUFFIX}",
        depends = config.prefixName(RUN_BENCHMARKS_TASKNAME)
    ) {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Executes benchmark for '${target.name}'"
        extensions.extraProperties.set("idea.internal.test", System.getProperty("idea.active"))

        val reportsDir = benchmarkReportsDir(config, target)
        val reportFile = reportsDir.resolve("${target.name}.json")

        val executableFile = nodeModulesDir.resolve(compilation.compileKotlinTask.outputFile.name)
        script = executableFile.absolutePath
        if (target.workingDir != null) {
            advanced {
                it.workingDir = File(target.workingDir)
            }
        }

        options("-r", "source-map-support/register")
        onlyIf { executableFile.exists() }

        arguments("-n", target.name)
        arguments("-r", reportFile.toString())
        config.iterations?.let { arguments("-i", it.toString()) }
        config.warmups?.let { arguments("-w", it.toString()) }
        config.iterationTime?.let { arguments("-it", it.toString()) }
        config.iterationTimeUnit?.let { arguments("-itu", it) }
        config.outputTimeUnit?.let { arguments("-otu", it) }
        config.mode?.let { arguments("-m", it) }

        config.includes.forEach {
            arguments("-I", it)
        }
        config.excludes.forEach {
            arguments("-E", it)
        }
        config.params.forEach { (param, values) ->
            values.forEach { value -> arguments("-P", "\"$param=$value\"") }
        }

        dependsOn("${target.name}${BenchmarksPlugin.BENCHMARK_DEPENDENCIES_SUFFIX}")
        doFirst {
            val ideaActive = (extensions.extraProperties.get("idea.internal.test") as? String)?.toBoolean() ?: false
            arguments("-t", if (ideaActive) "xml" else "text")
            reportsDir.mkdirs()
            logger.lifecycle("Running '${config.name}' benchmarks for '${target.name}'")
        }
    }
}

open class JsBenchmarkExec : NodeTask() {
/*
    @Option(option = "filter", description = "Configures the filter for benchmarks to run.")
    var filter: String? = null
*/
}


fun Project.createJsBenchmarkDependenciesTask(
    target: JsBenchmarkTarget,
    compilation: KotlinJsCompilation
) {
    val node = project.extensions.getByType(NodeExtension::class.java)
    val nodeModulesDir = node.node_modules
    val deployTask = task<Copy>("${target.name}${BenchmarksPlugin.BENCHMARK_DEPENDENCIES_SUFFIX}") {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Copy dependencies of benchmark for '${target.name}'"
        val configurationName = compilation.runtimeDependencyConfigurationName
        val configuration = configurations.getByName(configurationName)

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
        from(compilation.output) {
            transformSourceMaps(compilation.output.classesDirs)
        }

        from(dependencyFiles) {
            // We need to transform source maps from paths relative to output compilation folder to absolute paths.
            // The problem is that for dependencies at this point we don't know what task and in which project produced them.
            val dependencyOutputFolders = discoverOutputFolders(configuration)
            transformSourceMaps(dependencyOutputFolders)
        }
        into(nodeModulesDir)
        dependsOn("npmInstallBenchmarkJs")
        dependsOn(compilation.compileKotlinTaskName)
    }
    tasks.getByName(BenchmarksPlugin.ASSEMBLE_BENCHMARKS_TASKNAME).dependsOn(deployTask)
}

private fun Project.discoverOutputFolders(configuration: Configuration) = files(Callable {
    // This is magic. I'm sure I will not understand it in a week when I come back.
    // So lets walk through…
    // Configuration dependencies are just what's specified, so we need resolvedConfiguration
    // to get what they resolved to. 
    val resolvedConfiguration = configuration.resolvedConfiguration

    // Now we need module dependencies, but only to projects.
    // This is the only way I've found to separate projects from others – by their artifact component id
    // Which is of type `ProjectComponentIdentifier`, so here we build map from module id (maven coords) to project
    val resolvedModules = resolvedConfiguration.firstLevelModuleDependencies.toMutableSet()
    val projectIds = resolvedConfiguration.resolvedArtifacts.mapNotNull {
        when (val component = it.id.componentIdentifier) {
            is ProjectComponentIdentifier ->
                it.moduleVersion.id to rootProject.project(component.projectPath)
            else -> null
        }
    }.toMap()

    // Now we get all dependencies and combine them into `allModules` which is all transitive dependencies  
    val transitive = resolvedModules.flatMapTo(mutableSetOf()) { it.children }
    val allModules = (resolvedModules + transitive)

    // Now find those that are projects and select a configuration from each using resolved data 
    val resolvedProjectConfigurations = allModules.mapNotNull {
        projectIds[it.module.id]?.configurations?.getByName(it.configuration)
    }

    // Woa, we are almost there, we have configuration in the target project. 
    // Now the configuration itself provides artifacts that are JAR files already packed.
    // And it actually not the configuration itself, but something it extends and such.
    // So we need to get hierarchy of configurations, find those that can be resolved and get their task
    // dependencies. This way we find JAR tasks, but we need compile tasks! 
    // No problem, traverse task dependencies one more time! Hackery hack… 
    // 
    // Voila! Kotlin2JsCompile task is found, and we can just get its `outputFile`, to which source map is relative.
    val dependencyOutputFolders = resolvedProjectConfigurations.flatMap { config ->
        config.hierarchy.flatMap { c ->
            if (c.isCanBeResolved) {
                c.artifacts
                    .flatMap { it.buildDependencies.getDependencies(null) } // JAR tasks
                    .flatMap { it.taskDependencies.getDependencies(null) } // Compile tasks
                    .filterIsInstance<Kotlin2JsCompile>()
                    .map { it.outputFile.parentFile }
            } else
                emptyList()
        }
    }
    dependencyOutputFolders
})

/// Process .js.map files by replacing relative paths with absolute paths using provided roots
private fun Copy.transformSourceMaps(roots: FileCollection) {
    //println("TRANSFORM: roots: ${roots.files}")
    filesMatching("*.js.map") {
        //  println("FILE: ${it.sourcePath} (${it.relativeSourcePath})")
        it.filter { original ->
            buildString {
                var index = 0
                while (true) {
                    val beginMap = original.indexOf("\"../", index)
                    if (beginMap == -1) {
                        append(original.substring(index, original.length))
                        return@buildString
                    }
                    val endMap = original.indexOf("\"", beginMap + 1)
                    if (endMap == -1) {
                        append(original.substring(index, original.length))
                        return@buildString
                    }
                    append(original.substring(index, beginMap + 1))

                    val path = original.substring(beginMap + 1, endMap)
                    val absPath = resolveFromBases(roots, path)
                    append(absPath)
                    append("\"")
                    index = endMap + 1
                }

            }
        }
    }
}

fun resolveFromBases(files: Iterable<File>, path: String): String {
    val root = files.firstOrNull { it.resolve(path).exists() } ?: return path
    return root.resolve(path).normalize().toString()
}

