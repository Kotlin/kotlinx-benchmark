package kotlinx.benchmark.gradle

import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.*
import org.gradle.jvm.tasks.*
import java.io.*

fun Project.createJvmBenchmarkCompileTask(target: JvmBenchmarkTarget, compileClasspath: FileCollection) {
    val benchmarkBuildDir = benchmarkBuildDir(target)
    val compileTask = task<JavaCompile>(
        "${target.name}${BenchmarksPlugin.BENCHMARK_COMPILE_SUFFIX}",
        depends = BenchmarksPlugin.ASSEMBLE_BENCHMARKS_TASKNAME
    ) {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Compile JMH source files for '${target.name}'"
        dependsOn("${target.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}")
        classpath = compileClasspath
        source = fileTree("$benchmarkBuildDir/sources")
        destinationDir = file("$benchmarkBuildDir/classes")
    }

    task<Jar>(
        "${target.name}${BenchmarksPlugin.BENCHMARK_JAR_SUFFIX}",
        depends = BenchmarksPlugin.ASSEMBLE_BENCHMARKS_TASKNAME
    ) {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Build JAR for JMH compiled files for '${target.name}'"
        dependsOn("${target.name}${BenchmarksPlugin.BENCHMARK_COMPILE_SUFFIX}")
        conventionMapping.map("classifier") { "JMH" }
        manifest.attributes["Main-Class"] = "org.openjdk.jmh.Main"
        
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        
        from(project.provider {
            compileClasspath.map {
                when {
                    it.isDirectory -> it
                    it.exists() -> zipTree(it)
                    else -> files()
                }
            }
        })

        from(compileTask)
        from(file("$benchmarkBuildDir/resources"))
        destinationDirectory.set(File("$benchmarkBuildDir/jars"))
        archiveBaseName.set("${project.name}-${target.name}-jmh")
    }
}

fun Project.createJmhGenerationRuntimeConfiguration(name: String, jmhVersion: String): Configuration {
    // This configuration defines classpath for JMH generator, it should have everything available via reflection
    return configurations.create("$name${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}CP").apply {
        isVisible = false
        description = "JMH Generator Runtime Configuration for '$name'"

        val dependencies = this@createJmhGenerationRuntimeConfiguration.dependencies
        @Suppress("UnstableApiUsage")
        (defaultDependencies {
            it.add(dependencies.create("${BenchmarksPlugin.JMH_GENERATOR_DEPENDENCY}$jmhVersion"))
        })
    }
}

fun Project.createJvmBenchmarkGenerateSourceTask(
    target: BenchmarkTarget,
    workerClasspath: FileCollection,
    compileClasspath: FileCollection,
    compilationTask: String,
    compilationOutput: FileCollection
) {
    val benchmarkBuildDir = benchmarkBuildDir(target)
    task<JmhBytecodeGeneratorTask>("${target.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}") {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Generate JMH source files for '${target.name}'"
        dependsOn(compilationTask)
        runtimeClasspath = workerClasspath
        inputCompileClasspath = compileClasspath
        inputClassesDirs = compilationOutput
        outputResourcesDir = file("$benchmarkBuildDir/resources")
        outputSourcesDir = file("$benchmarkBuildDir/sources")
    }
}

fun Project.createJvmBenchmarkExecTask(
    config: BenchmarkConfiguration,
    target: JvmBenchmarkTarget,
    runtimeClasspath: FileCollection
) {
    // TODO: add working dir parameter?
    task<JavaExec>(
        "${target.name}${config.capitalizedName()}${BenchmarksPlugin.BENCHMARK_EXEC_SUFFIX}",
        depends = config.prefixName(BenchmarksPlugin.RUN_BENCHMARKS_TASKNAME)
    ) {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Execute benchmark for '${target.name}'"
        extensions.extraProperties.set("idea.internal.test", project.getSystemProperty("idea.active"))

        val benchmarkBuildDir = benchmarkBuildDir(target)
        val reportsDir = benchmarkReportsDir(config, target)
        val reportFile = reportsDir.resolve("${target.name}.${config.reportFileExt()}")
        main = "kotlinx.benchmark.jvm.JvmBenchmarkRunnerKt"

        if (target.workingDir != null)
            workingDir = File(target.workingDir)

        classpath(
            file("$benchmarkBuildDir/classes"),
            file("$benchmarkBuildDir/resources"),
            runtimeClasspath
        )


        dependsOn("${target.name}${BenchmarksPlugin.BENCHMARK_COMPILE_SUFFIX}")
        doFirst {
            val ideaActive = (extensions.extraProperties.get("idea.internal.test") as? String)?.toBoolean() ?: false
            args(writeParameters(target.name, reportFile, if (ideaActive) "xml" else "text", config))
            reportsDir.mkdirs()
            logger.lifecycle("Running '${config.name}' benchmarks for '${target.name}'")
        }
    }
}
