package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.file.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.*
import org.gradle.util.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import java.io.*

@Suppress("unused")
class BenchmarksPlugin : Plugin<Project> {
    companion object {
        private val GRADLE_NEW = GradleVersion.current() >= GradleVersion.version("4.9-rc-1")

        const val BENCHMARKS_TASK_GROUP = "benchmark"
        const val BENCHMARK_EXTENSION_NAME = "benchmark"

        const val BENCHMARK_GENERATE_SUFFIX = "BenchmarkGenerate"
        const val BENCHMARK_COMPILE_SUFFIX = "BenchmarkCompile"
        const val BENCHMARK_EXEC_SUFFIX = "Benchmark"

        const val JMH_CORE_DEPENDENCY = "org.openjdk.jmh:jmh-core:"
        const val JMH_GENERATOR_DEPENDENCY = "org.openjdk.jmh:jmh-generator-bytecode:"
    }

    override fun apply(project: Project) {
        // DO NOT introduce a variable, extension values should be only read in a task or afterEvaluate
        // Otherwise it will not contain relevant data
        project.extensions.create(BENCHMARK_EXTENSION_NAME, BenchmarksExtension::class.java, project)

        // Create empty task to serve as a root for all benchmarks in a project
        project.task<DefaultTask>("benchmark") {
            group = BENCHMARKS_TASK_GROUP
            description = "Runs all benchmarks in a project"
        }

        project.afterEvaluate {
            val extension = project.extensions.getByType(BenchmarksExtension::class.java)
            extension.configurations.all { config ->
                project.processConfiguration(extension, config)
            }
        }
    }

    private fun Project.processConfiguration(extension: BenchmarksExtension, config: BenchmarkConfiguration) {
        val mpp = project.extensions.findByType(KotlinMultiplatformExtension::class.java)
        if (mpp != null) {
            configureKotlinMultiplatform(config, mpp, extension)
            return // TODO: test with Java plugin
        }

        plugins.withType(JavaPlugin::class.java) {
            configureJavaPlugin(config, extension)
        }
    }

    private fun Project.configureJavaPlugin(config: BenchmarkConfiguration, extension: BenchmarksExtension) {
        project.logger.info("Configuring benchmarks for '${config.name}' using Java")

        // get configure source set and add JMH core dependency to it
        val sourceSet = configureJavaSourceSet(this, config)

        // we need JMH generator runtime configuration for each BenchmarkConfiguration since version can be different
        val jmhRuntimeConfiguration = createJmhGenerationRuntimeConfiguration(
            this,
            config,
            sourceSet.runtimeClasspath
        )

        // Create a task that will process output bytecode and generate benchmark Java source code
        createBenchmarkGenerateSourceTask(
            extension,
            config,
            jmhRuntimeConfiguration,
            sourceSet.classesTaskName,
            sourceSet.output
        )

        // Create a task that will compile generated Java source code into class files
        createBenchmarkCompileTask(extension, config, sourceSet.runtimeClasspath)

        // Create a task that will execute benchmark code
        createBenchmarkExecTask(extension, config, sourceSet.runtimeClasspath)
    }

    private fun Project.configureKotlinMultiplatform(
        config: BenchmarkConfiguration,
        mpp: KotlinMultiplatformExtension,
        extension: BenchmarksExtension
    ) {
        project.logger.info("Configuring benchmarks for '${config.name}' using Multiplatform Kotlin")

        val compilations = mpp.targets.flatMap { it.compilations.filterIsInstance<KotlinJvmCompilation>() }
        // TODO: update mpp plugin API to remove this hack (have something like compilation base name)
        val compilation = compilations.singleOrNull { it.apiConfigurationName.removeSuffix("Api") == config.name }
        if (compilation == null) {
            logger.error("Problem: Cannot find a JVM benchmark compilation '${config.name}'")
            return // ignore
        }
        configureMppSourceSet(this, config, compilation)

        val jmhRuntimeConfiguration = createJmhGenerationRuntimeConfiguration(
            this,
            config,
            compilation.runtimeDependencyFiles
        )

        createBenchmarkGenerateSourceTask(
            extension,
            config,
            jmhRuntimeConfiguration,
            compilation.compileAllTaskName,
            compilation.output.allOutputs
        )
        val runtimeClasspath = compilation.output.allOutputs + compilation.runtimeDependencyFiles
        createBenchmarkCompileTask(extension, config, runtimeClasspath)
        createBenchmarkExecTask(extension, config, runtimeClasspath)
    }

    private fun configureMppSourceSet(
        project: Project,
        config: BenchmarkConfiguration,
        compilation: KotlinJvmCompilation
    ) {
        val dependencies = project.dependencies
        val jmhCore = dependencies.create("$JMH_CORE_DEPENDENCY${config.jmhVersion}")
        compilation.dependencies {
            implementation(jmhCore)
        }
    }

    private fun configureJavaSourceSet(project: Project, config: BenchmarkConfiguration): SourceSet {
        val dependencies = project.dependencies
        val javaConvention = project.convention.getPlugin(JavaPluginConvention::class.java)

        // Add dependency to JMH core library to the source set designated by config.name
        val jmhCore = dependencies.create("$JMH_CORE_DEPENDENCY${config.jmhVersion}")
        val configurationRoot = if (GRADLE_NEW) "implementation" else "compile"
        val dependencyConfiguration =
            if (config.name == "main") configurationRoot else "${config.name}${configurationRoot.capitalize()}"
        dependencies.add(dependencyConfiguration, jmhCore)
        return javaConvention.sourceSets.getByName(config.name)
    }

    private fun createJmhGenerationRuntimeConfiguration(
        project: Project,
        config: BenchmarkConfiguration,
        classPath: FileCollection
    ): Configuration {
        // This configuration defines classpath for JMH generator, it should have everything available via reflection
        return project.configurations.create("${config.name}$BENCHMARK_GENERATE_SUFFIX").apply {
            isVisible = false
            description = "JMH Generator Runtime Configuration for '${config.name}'"

            @Suppress("UnstableApiUsage")
            defaultDependencies {
                it.add(project.dependencies.create("$JMH_GENERATOR_DEPENDENCY${config.jmhVersion}"))
                // TODO: runtimeClasspath or compileClasspath? how to avoid premature resolve()?
                it.add(project.dependencies.create(classPath))
            }
        }
    }

    private fun Project.createBenchmarkGenerateSourceTask(
        extension: BenchmarksExtension,
        config: BenchmarkConfiguration,
        classpath: Configuration,
        compilationTask: String,
        compilationOutput: FileCollection
    ) {
        val benchmarkBuildDir = benchmarkBuildDir(extension, config)
        task<JmhBytecodeGeneratorTask>("${config.name}$BENCHMARK_GENERATE_SUFFIX") {
            group = BENCHMARKS_TASK_GROUP
            description = "Generate JMH source files for '${config.name}'"
            dependsOn(compilationTask)
            runtimeClasspath = classpath.resolve()
            inputClassesDirs = compilationOutput
            outputResourcesDir = file("$benchmarkBuildDir/resources")
            outputSourcesDir = file("$benchmarkBuildDir/sources")
        }
    }

    private fun Project.createBenchmarkCompileTask(
        extension: BenchmarksExtension,
        config: BenchmarkConfiguration,
        compileClasspath: FileCollection
    ) {
        val benchmarkBuildDir = benchmarkBuildDir(extension, config)
        task<JavaCompile>("${config.name}$BENCHMARK_COMPILE_SUFFIX") {
            group = BENCHMARKS_TASK_GROUP
            description = "Compile JMH source files for '${config.name}'"
            dependsOn("${config.name}$BENCHMARK_GENERATE_SUFFIX")
            classpath = compileClasspath
            setSource(file("$benchmarkBuildDir/sources")) // TODO: try using FileTree since 4.0
            destinationDir = file("$benchmarkBuildDir/classes")
        }
    }

    private fun Project.createBenchmarkExecTask(
        extension: BenchmarksExtension,
        config: BenchmarkConfiguration,
        runtimeClasspath: FileCollection
    ) {
        val benchmarkBuildDir = benchmarkBuildDir(extension, config)
        task<JavaExec>("${config.name}$BENCHMARK_EXEC_SUFFIX", depends = "benchmark") {
            group = BENCHMARKS_TASK_GROUP
            description = "Execute benchmark for '${config.name}'"
            main = "org.openjdk.jmh.Main"
            classpath(
                file("$benchmarkBuildDir/classes"),
                file("$benchmarkBuildDir/resources"),
                runtimeClasspath
            )
            dependsOn("${config.name}$BENCHMARK_COMPILE_SUFFIX")
            tasks.getByName("benchmark").dependsOn(this)
        }
    }

    private fun Project.benchmarkBuildDir(extension: BenchmarksExtension, config: BenchmarkConfiguration): File? {
        return file("$buildDir/${extension.buildDir}/${config.name}")
    }

    private inline fun <reified T : Task> Project.task(
        name: String,
        depends: String? = null,
        noinline configuration: T.() -> Unit
    ) {
        when {
            GRADLE_NEW -> {
                @Suppress("UnstableApiUsage")
                val task = tasks.register(name, T::class.java, Action(configuration))
                if (depends != null) {
                    tasks.getByName(depends).dependsOn(task)
                }
            }
            else -> {
                val task = tasks.create(name, T::class.java, Action(configuration))
                if (depends != null) {
                    tasks.getByName(depends).dependsOn(task)
                }
            }
        }
    }
}
