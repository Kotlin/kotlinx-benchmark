package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.targets.js.dsl.*
import org.jetbrains.kotlin.gradle.targets.js.ir.*
import org.jetbrains.kotlin.serialization.js.ModuleKind

@KotlinxBenchmarkPluginInternalApi
fun Project.processJsCompilation(target: JsBenchmarkTarget) {
    project.logger.info("Configuring benchmarks for '${target.name}' using Kotlin/JS")
    val compilation = target.compilation

    createJsBenchmarkGenerateSourceTask(target, compilation)

    val benchmarkCompilation = createJsBenchmarkCompileTask(target)

    target.extension.configurations.forEach {
        createJsEngineBenchmarkExecTask(it, target, benchmarkCompilation)
    }
}

private fun Project.createJsBenchmarkCompileTask(target: JsBenchmarkTarget): KotlinJsIrCompilation {
    val compilation = target.compilation
    val benchmarkBuildDir = benchmarkBuildDir(target)
    val benchmarkCompilation =
        compilation.target.compilations.create(target.name + BenchmarksPlugin.BENCHMARK_COMPILATION_SUFFIX) as KotlinJsIrCompilation

    (compilation.target as KotlinJsTargetDsl).apply {
        //force to create executable: required for IR, do nothing on Legacy
        binaries.executable(benchmarkCompilation)
    }

    benchmarkCompilation.apply {
        val sourceSet = kotlinSourceSets.single()

        sourceSet.kotlin.setSrcDirs(files("$benchmarkBuildDir/sources"))
        sourceSet.resources.setSrcDirs(files())

        associateWith(compilation)

        sourceSet.dependencies {
            implementation(npm("benchmark", "*"))
            runtimeOnly(npm("source-map-support", "*"))
        }

        compileTaskProvider.configure {
            it.apply {
                group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
                description = "Compile JS benchmark source files for '${target.name}'"

                //TODO: fix destination dir after KT-29711 is fixed
                //println("JS: ${kotlinOptions.outputFile}")
                //destinationDir = file("$benchmarkBuildDir/classes")
                dependsOn("${target.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}")

                compilerOptions {
                    sourceMap.set(true)
                    compilation.kotlinOptions.moduleKind?.let {
                        moduleKind.set(JsModuleKind.fromKind(it))
                    }
                }
            }
        }
    }
    return benchmarkCompilation
}

private fun Project.createJsBenchmarkGenerateSourceTask(
    target: JsBenchmarkTarget,
    compilationOutput: KotlinJsIrCompilation
) {
    val benchmarkBuildDir = benchmarkBuildDir(target)
    task<JsSourceGeneratorTask>("${target.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}") {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Generate JS source files for '${target.name}'"
        title = target.name
        useBenchmarkJs = target.jsBenchmarksExecutor == JsBenchmarksExecutor.BenchmarkJs
        inputClassesDirs = compilationOutput.output.classesDirs
        inputDependencies = compilationOutput.runtimeDependencyFiles
        outputResourcesDir = file("$benchmarkBuildDir/resources")
        outputSourcesDir = file("$benchmarkBuildDir/sources")
    }
}
