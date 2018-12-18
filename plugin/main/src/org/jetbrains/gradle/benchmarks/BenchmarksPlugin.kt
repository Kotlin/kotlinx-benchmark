package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.file.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.*
import org.gradle.util.*
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
            extension.configurations.all { 
                project.processConfiguration(extension, it) 
            }
        }
    }

    private fun Project.processConfiguration(extension: BenchmarksExtension, config: BenchmarkConfiguration) {
        plugins.withType(JavaPlugin::class.java) {
            logger.info("Creating JMH benchmark tasks for '${config.name}'")

            // get configure source set and add JMH core dependency to it
            val sourceSet = configureJavaSourceSet(this, config)

            // we need JMH generator runtime configuration for each BenchmarkConfiguration since version can be different
            val jmhRuntimeConfiguration = createJmhGenerationRuntimeConfiguration(this, config)

            // Create a task that will process output bytecode and generate benchmark Java source code
            createBenchmarkGenerateSourceTask(
                extension,
                config,
                jmhRuntimeConfiguration,
                sourceSet.classesTaskName,
                sourceSet.output 
            )

            // Create a task that will compile generated Java source code into class files
            createBenchmarkCompileTask(extension, config, sourceSet)

            // Create a task that will execute benchmark code
            createBenchmarkExecTask(extension, config, sourceSet)
        }
    }

    private fun configureJavaSourceSet(project: Project, config: BenchmarkConfiguration): SourceSet {
        val dependencies = project.dependencies
        val javaConvention = project.convention.getPlugin(JavaPluginConvention::class.java)

        // Add dependency to JMH core library to the source set designated by config.name
        val sourceSet = javaConvention.sourceSets.getByName(config.name)
        val jmhCore = dependencies.create("$JMH_CORE_DEPENDENCY${config.jmhVersion}")
        val configurationRoot = if (GRADLE_NEW) "implementation" else "compile"
        val dependencyConfiguration = if (config.name == "main") configurationRoot else "${config.name}${configurationRoot.capitalize()}"
        dependencies.add(dependencyConfiguration, jmhCore)
        return sourceSet
    }

    private fun createJmhGenerationRuntimeConfiguration(project: Project, config: BenchmarkConfiguration): Configuration {
        // This configuration defines classpath for JMH generator, it should have everything available via reflection
        
        val javaConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
        val sourceSet = javaConvention.sourceSets.getByName(config.name)
        return project.configurations.create("${config.name}$BENCHMARK_GENERATE_SUFFIX").apply {
            isVisible = false
            description = "JMH Generator Runtime Configuration for '${config.name}'"

            @Suppress("UnstableApiUsage")
            defaultDependencies {
                it.add(project.dependencies.create("$JMH_GENERATOR_DEPENDENCY${config.jmhVersion}"))
                // TODO: runtimeClasspath or compileClasspath? how to avoid premature resolve()?
                it.add(project.dependencies.create(sourceSet.runtimeClasspath))
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
            description = "Generate JMH source files for ${config.name}"
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
        sourceSet: SourceSet
    ) {
        val benchmarkBuildDir = benchmarkBuildDir(extension, config)
        task<JavaCompile>("${config.name}$BENCHMARK_COMPILE_SUFFIX") {
            group = BENCHMARKS_TASK_GROUP
            description = "Compile JMH source files for $sourceSet"
            dependsOn("${config.name}$BENCHMARK_GENERATE_SUFFIX")
            classpath = sourceSet.runtimeClasspath
            setSource(file("$benchmarkBuildDir/sources")) // TODO: try using FileTree since 4.0
            destinationDir = file("$benchmarkBuildDir/classes")
        }
    }

    private fun Project.createBenchmarkExecTask(
        extension: BenchmarksExtension,
        config: BenchmarkConfiguration,
        sourceSet: SourceSet
    ) {
        val benchmarkBuildDir = benchmarkBuildDir(extension, config)
        task<JavaExec>("${config.name}$BENCHMARK_EXEC_SUFFIX") {
            main = "org.openjdk.jmh.Main" 
            classpath(file("$benchmarkBuildDir/classes"), file("$benchmarkBuildDir/resources"), sourceSet.runtimeClasspath)
            dependsOn("${config.name}$BENCHMARK_COMPILE_SUFFIX")
            tasks.getByName("benchmark").dependsOn(this)
        }
    }

    private fun Project.benchmarkBuildDir(extension: BenchmarksExtension, config: BenchmarkConfiguration): File? {
        return file("$buildDir/${extension.buildDir}/${config.name}")
    }

    private inline fun <reified T : Task> Project.task(name: String, noinline configuration: T.() -> Unit) {
        when {
            GRADLE_NEW -> {
                @Suppress("UnstableApiUsage")
                tasks.register(name, T::class.java, Action(configuration))
            }
            else -> {
                tasks.create(name, T::class.java, Action(configuration))
            }
        }
    }
}
