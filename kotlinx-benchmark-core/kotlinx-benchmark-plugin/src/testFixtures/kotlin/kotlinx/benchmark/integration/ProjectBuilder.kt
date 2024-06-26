package kotlinx.benchmark.integration

import kotlinx.benchmark.gradle.internal.BenchmarksPluginConstants.BENCHMARK_PLUGIN_VERSION
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString

class ProjectBuilder {
    private val configurations = mutableMapOf<String, BenchmarkConfiguration>()
    private val targets = mutableMapOf<String, BenchmarkTarget>()

    var kotlinVersion: String = System.getProperty("kotlin_version")

    fun configuration(name: String, configuration: BenchmarkConfiguration.() -> Unit = {}) {
        configurations[name] = BenchmarkConfiguration().apply(configuration)
    }

    fun register(name: String, configuration: BenchmarkTarget.() -> Unit = {}) {
        targets[name] = BenchmarkTarget().apply(configuration)
    }

    fun build(original: String): String {

        val script =
            """
benchmark {
    configurations {
        ${configurations.flatMap { it.value.lines(it.key) }.joinToString("\n        ")}
    }
    targets {
        ${targets.flatMap { it.value.lines(it.key) }.joinToString("\n        ")}
    }
}
            """.trimIndent()

        return generateBuildScript(kotlinVersion) + "\n\n" + original + "\n\n" + script
    }
}

private fun generateBuildScript(kotlinVersion: String): String {

    val devMavenRepositoryPaths = System.getProperty("devMavenRepositories")
        ?.split(",")
        ?.map { Path(it) }
        ?: error("missing devMavenRepositories system property")

    // use locally published versions via `devMavenPublish`
    val devMavenRepositories = devMavenRepositoryPaths
        .withIndex()
        .joinToString("\n") { (i, repoPath) ->
            // Must be compatible with both Groovy and Kotlin DSL.
            """
            |maven {
            |    setUrl("${repoPath.invariantSeparatorsPathString}")
            |    name = "DevMavenRepo${i}"
            |}
            """.trimMargin()
        }

    return """
    buildscript {
        repositories {
            $devMavenRepositories
            mavenCentral()
        }
        dependencies {
            classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion'
            classpath 'org.jetbrains.kotlinx:kotlinx-benchmark-plugin:$BENCHMARK_PLUGIN_VERSION'
        }
    }
    
    apply plugin: 'kotlin-multiplatform'
    apply plugin: 'org.jetbrains.kotlinx.benchmark'
    
    repositories {
        $devMavenRepositories
        mavenCentral()
    }
    """.trimIndent()
}
