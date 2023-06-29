package kotlinx.benchmark.integration

class ProjectBuilder {
    private val configurations = mutableMapOf<String, BenchmarkConfiguration>()
    private val targets = mutableMapOf<String, BenchmarkTarget>()

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

        return buildScript + "\n\n" + original + "\n\n" + script
    }
}

private val kotlin_repo = System.getProperty("kotlin_repo_url").let {
    if (it.isNullOrBlank()) "" else "maven { url '$it' }"
}

private val buildScript =
    """
    buildscript {
        repositories {
            $kotlin_repo
            maven { url '${System.getProperty("plugin_repo_url")}' }
            mavenCentral()
        }
        dependencies {
            classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:${System.getProperty("kotlin_version")}'
            classpath 'org.jetbrains.kotlinx:kotlinx-benchmark-plugin:0.5.0-SNAPSHOT'
        }
    }
    
    apply plugin: 'kotlin-multiplatform'
    apply plugin: 'org.jetbrains.kotlinx.benchmark'
    
    repositories {
        $kotlin_repo
        maven { url '${System.getProperty("runtime_repo_url")}' }
        mavenCentral()
    }
    """.trimIndent()
