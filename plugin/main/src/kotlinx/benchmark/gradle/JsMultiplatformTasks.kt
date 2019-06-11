package kotlinx.benchmark.gradle

import kotlinx.team.infra.node.*
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

fun Project.processJsCompilation(target: JsBenchmarkTarget) {
    if (!plugins.hasPlugin(NodePlugin::class.java)) {
        logger.info("Enabling node plugin in $this")
        pluginManager.apply(NodePlugin::class.java)
    }

    project.logger.info("Configuring benchmarks for '${target.name}' using Kotlin/JS")
    val compilation = target.compilation

    configureMultiplatformJsCompilation(target)

    createJsBenchmarkInstallTask()
    createJsBenchmarkGenerateSourceTask(target, compilation)

    val benchmarkCompilation = createJsBenchmarkCompileTask(target)
    createJsBenchmarkDependenciesTask(target, benchmarkCompilation)
    target.extension.configurations.forEach {
        createJsBenchmarkExecTask(it, target, benchmarkCompilation)
    }
}

private fun Project.createJsBenchmarkCompileTask(target: JsBenchmarkTarget): KotlinJsCompilation {
    val compilation = target.compilation
    val benchmarkBuildDir = benchmarkBuildDir(target)
    val benchmarkCompilation =
        compilation.target.compilations.create(BenchmarksPlugin.BENCHMARK_COMPILATION_NAME) as KotlinJsCompilation

    benchmarkCompilation.apply {
        val sourceSet = kotlinSourceSets.single()
        sourceSet.kotlin.setSrcDirs(files("$benchmarkBuildDir/sources"))
        sourceSet.resources.setSrcDirs(files())
        sourceSet.dependencies {
            implementation(compilation.compileDependencyFiles)
            implementation(compilation.output.allOutputs)
        }
        compileKotlinTask.apply {
            group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
            description = "Compile JS benchmark source files for '${target.name}'"

            //TODO: fix destination dir after KT-29711 is fixed
            //println("JS: ${kotlinOptions.outputFile}")
            //destinationDir = file("$benchmarkBuildDir/classes")
            dependsOn("${target.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}")

            kotlinOptions.apply {
                sourceMap = true
                moduleKind = "umd"
            }
        }
    }
    return benchmarkCompilation
}

private fun Project.createJsBenchmarkGenerateSourceTask(
    target: JsBenchmarkTarget,
    compilationOutput: KotlinJsCompilation
) {
    val benchmarkBuildDir = benchmarkBuildDir(target)
    task<JsSourceGeneratorTask>("${target.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}") {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Generate JS source files for '${target.name}'"
        title = target.name
        inputClassesDirs = compilationOutput.output.allOutputs
        inputDependencies = compilationOutput.compileDependencyFiles
        outputResourcesDir = file("$benchmarkBuildDir/resources")
        outputSourcesDir = file("$benchmarkBuildDir/sources")
    }
}

private fun Project.configureMultiplatformJsCompilation(target: JsBenchmarkTarget) {
    // Add runtime library as an implementation dependency to the specified compilation
    val runtime = dependencies.create("${BenchmarksPlugin.RUNTIME_DEPENDENCY_BASE}-js:${target.extension.version}")

    target.compilation.dependencies {
        //implementation(runtime)
    }
}
