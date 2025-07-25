package kotlinx.benchmark.gradle

import kotlinx.benchmark.gradle.internal.BenchmarkDependencies
import kotlinx.benchmark.gradle.internal.BenchmarksPluginConstants
import kotlinx.benchmark.gradle.internal.BenchmarksPluginConstants.DEFAULT_KOTLIN_COMPILER_VERSION
import kotlinx.benchmark.gradle.internal.BenchmarksPluginConstants.MIN_SUPPORTED_GRADLE_VERSION
import kotlinx.benchmark.gradle.internal.BenchmarksPluginConstants.MIN_SUPPORTED_KOTLIN_VERSION
import kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi
import org.gradle.api.*
import org.gradle.api.provider.*
import org.gradle.util.GradleVersion
import javax.inject.Inject

@Suppress("unused")
abstract class BenchmarksPlugin
@KotlinxBenchmarkPluginInternalApi
@Inject
constructor(
    private val providers: ProviderFactory,
) : Plugin<Project> {

    companion object {
        const val PLUGIN_ID = "org.jetbrains.kotlinx.benchmark"

        const val PLUGIN_VERSION = BenchmarksPluginConstants.BENCHMARK_PLUGIN_VERSION

        const val BENCHMARKS_TASK_GROUP = "benchmark"
        const val BENCHMARK_EXTENSION_NAME = "benchmark"

        const val RUN_BENCHMARKS_TASKNAME = "benchmark"
        const val ASSEMBLE_BENCHMARKS_TASKNAME = "assembleBenchmarks"

        //region Internal constants
        @KotlinxBenchmarkPluginInternalApi
        const val BENCHMARK_GENERATE_SUFFIX = "BenchmarkGenerate"

        @KotlinxBenchmarkPluginInternalApi
        const val BENCHMARK_COMPILE_SUFFIX = "BenchmarkCompile"

        @KotlinxBenchmarkPluginInternalApi
        const val BENCHMARK_JAR_SUFFIX = "BenchmarkJar"

        @KotlinxBenchmarkPluginInternalApi
        const val BENCHMARK_EXEC_SUFFIX = "Benchmark"

        @KotlinxBenchmarkPluginInternalApi
        const val BENCHMARK_COMPILATION_SUFFIX = "Benchmark"

        @KotlinxBenchmarkPluginInternalApi
        const val JMH_CORE_DEPENDENCY = "org.openjdk.jmh:jmh-core"

        @KotlinxBenchmarkPluginInternalApi
        const val JMH_GENERATOR_DEPENDENCY = "org.openjdk.jmh:jmh-generator-bytecode:"
        //endregion
    }

    override fun apply(project: Project) = project.run {
        // DO NOT use properties of an extension immediately, it will not contain any user-specified data
        val extension = createBenchmarksExtension(project)

        if (GradleVersion.current() < GradleVersion.version(MIN_SUPPORTED_GRADLE_VERSION)) {
            logger.error("JetBrains Gradle Benchmarks plugin requires Gradle version $MIN_SUPPORTED_GRADLE_VERSION or higher")
            return // TODO: Do we need to fail build at this point or just ignore benchmarks?
        }

        plugins.withType(org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin::class.java) { kotlinPlugin ->
            logger.info("Detected Kotlin plugin version '${kotlinPlugin.pluginVersion}'")
            if (getKotlinVersion(kotlinPlugin.pluginVersion) < getKotlinVersion(MIN_SUPPORTED_KOTLIN_VERSION)) {
                logger.error("JetBrains Gradle Benchmarks plugin requires Kotlin version $MIN_SUPPORTED_KOTLIN_VERSION or higher")
            }
            extension.kotlinCompilerVersion.set(kotlinPlugin.pluginVersion)
        }

        // Create a lifecycle task that will depend on all benchmark building tasks to build all benchmarks in a project
        val assembleBenchmarks = task<DefaultTask>(ASSEMBLE_BENCHMARKS_TASKNAME) {
            group = BENCHMARKS_TASK_GROUP
            description = "Generate and build all benchmarks in a project"
        }

        val benchmarkDependencies = BenchmarkDependencies(project, extension)

        configureBenchmarkTaskConventions(project, benchmarkDependencies)

        // TODO: Design configuration avoidance
        // I currently don't know how to do it correctly yet, so materialize all tasks after project evaluation.
        afterEvaluate {
            extension.configurations.forEach {
                // Create a lifecycle task that will depend on all benchmark execution tasks to run all benchmarks in a project
                task<DefaultTask>(it.prefixName(RUN_BENCHMARKS_TASKNAME)) {
                    group = BENCHMARKS_TASK_GROUP
                    description = "Execute all benchmarks in a project"
                    extensions.extraProperties.set("idea.internal.test", getSystemProperty("idea.active"))

                    // Force all benchmarks runner to first build all benchmarks to ensure it won't spend time
                    // running some benchmarks when other will fail to compile
                    // Individual benchmarks depend on their respective building tasks for fast turnaround
                    dependsOn(assembleBenchmarks)
                }

            }

            processConfigurations(extension)
        }
    }

    private fun createBenchmarksExtension(project: Project): BenchmarksExtension {
        return project.extensions.create(BENCHMARK_EXTENSION_NAME, BenchmarksExtension::class.java, project).apply {
            kotlinCompilerVersion.convention(DEFAULT_KOTLIN_COMPILER_VERSION)
        }
    }

    private fun Project.processConfigurations(extension: BenchmarksExtension) {
        // Calling `all` on NDOC causes all items to materialize and be configured
        extension.targets.all { config ->
            when (config) {
                is JavaBenchmarkTarget -> processJavaSourceSet(config)
                is KotlinJvmBenchmarkTarget -> processJvmCompilation(config)
                is JsBenchmarkTarget -> processJsCompilation(config)
                is WasmBenchmarkTarget -> processWasmCompilation(config)
                is NativeBenchmarkTarget -> processNativeCompilation(config)
            }
        }
        extension.checkConflictingJmhVersions()
    }

    private fun configureBenchmarkTaskConventions(
        project: Project,
        benchmarkDependencies: BenchmarkDependencies,
    ) {
        project.tasks.withType(NativeSourceGeneratorTask::class.java).configureEach {
            it.runtimeClasspath.from(benchmarkDependencies.benchmarkGeneratorResolver)
        }
        project.tasks.withType(WasmSourceGeneratorTask::class.java).configureEach {
            it.runtimeClasspath.from(benchmarkDependencies.benchmarkGeneratorResolver)
        }
        project.tasks.withType(JsSourceGeneratorTask::class.java).configureEach {
            it.runtimeClasspath.from(benchmarkDependencies.benchmarkGeneratorResolver)
        }
    }
}

private fun getKotlinVersion(kotlinVersion: String): KotlinVersion {
    val (major, minor) = kotlinVersion
        .split('.')
        .take(2)
        .map { it.toInt() }
    val patch = kotlinVersion.substringAfterLast('.').substringBefore('-').toInt()
    return KotlinVersion(major, minor, patch)
}
