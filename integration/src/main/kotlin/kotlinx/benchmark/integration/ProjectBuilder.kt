package kotlinx.benchmark.integration

class ProjectBuilder(private val isKts: Boolean = false) {
    private val configurations = mutableMapOf<String, BenchmarkConfiguration>()
    private val targets = mutableMapOf<String, BenchmarkTarget>()

    var kotlinVersion: String = System.getProperty("kotlin_version")

    var kotlinxBenchmarksVersion: String = "0.5.0-SNAPSHOT"

    fun configuration(name: String, configuration: BenchmarkConfiguration.() -> Unit = {}) {
        configurations[name] = BenchmarkConfiguration().apply(configuration)
    }

    fun register(name: String, configuration: BenchmarkTarget.() -> Unit = {}) {
        targets[name] = BenchmarkTarget().apply(configuration)
    }

    fun build(original: String) = if (isKts) buildKts(original) else buildGroovy(original)

    private fun buildKts(original: String): String =
        """
        buildscript {
            repositories {
                $kotlinRepoURL
                maven("${System.getProperty("plugin_repo_url")}")
                mavenCentral()
            }
            dependencies {
                classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
                classpath("org.jetbrains.kotlinx:kotlinx-benchmark-plugin:$kotlinxBenchmarksVersion")
            }
        }
        
        plugins {
            kotlin("multiplatform") version "$kotlinVersion"
            id("org.jetbrains.kotlinx.benchmark") version "$kotlinxBenchmarksVersion"
        }
        
        repositories {
            $kotlinRepoURL
            maven("${System.getProperty("runtime_repo_url")}")
            mavenCentral()
        }
        
        $original

        benchmark {
            configurations {
                ${formatConfigurations()}
            }
            targets {
                ${formatTargets()}
            }      
        }
        """.trimIndent()

    private fun buildGroovy(original: String): String =
        """
        buildscript {
            repositories {
                $kotlinRepoURL
                maven { url '${System.getProperty("plugin_repo_url")}' }
                mavenCentral()
            }
            dependencies {
                classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion'
                classpath 'org.jetbrains.kotlinx:kotlinx-benchmark-plugin:$kotlinxBenchmarksVersion'
            }
        }
        
        apply plugin: 'kotlin-multiplatform'
        apply plugin: 'org.jetbrains.kotlinx.benchmark'
        
        repositories {
            $kotlinRepoURL
            maven { url '${System.getProperty("runtime_repo_url")}' }
            mavenCentral()
        }

        $original
        
        benchmark {
            configurations {
                ${formatConfigurations()}
            }
            targets {
                ${formatTargets()}
            }
        }
        """.trimIndent()

    private val kotlinRepoURL = System.getProperty("kotlin_repo_url").let {
        when {
            it.isNullOrBlank() -> ""
            isKts -> "maven(\"$it\")"
            else -> "maven { url '$it' }"
        }
    }

    private fun formatTargets() = targets.flatMap { it.value.lines(it.key) }.joinToStringFormatted(8)

    private fun formatConfigurations() = configurations.flatMap { it.value.lines(it.key) }.joinToStringFormatted(8)

    private fun List<String>.joinToStringFormatted(tabulation: Int) = this.joinToString(
        separator = System.lineSeparator(),
        transform = { " ".repeat(tabulation) + it }
    )
}