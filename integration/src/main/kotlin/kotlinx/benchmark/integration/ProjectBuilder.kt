package kotlinx.benchmark.integration

class ProjectBuilder {
    private val configurations = mutableMapOf<String, BenchmarkConfiguration>()

    var kotlinVersion: String = System.getProperty("kotlin_version")
    var jvmToolchain: Int = 8

    fun configuration(name: String, configuration: BenchmarkConfiguration.() -> Unit = {}) {
        configurations[name] = BenchmarkConfiguration().apply(configuration)
    }

    fun build(original: String): String {

        val script =
            """
benchmark {
    configurations {
        ${configurations.flatMap { it.value.lines(it.key) }.joinToString("\n        ")}
    }
}
            """.trimIndent()

        return generateBuildScript(kotlinVersion, jvmToolchain) + "\n\n" + original + "\n\n" + script
    }
}

private val kotlin_repo = System.getProperty("kotlin_repo_url")?.let {
    "maven { url '$it' }"
}.orEmpty()

private val plugin_repo_url = System.getProperty("plugin_repo_url")!!.let {
    "maven { url '$it' }"
}

private val runtime_repo_url = System.getProperty("runtime_repo_url")!!.let {
    "maven { url '$it' }"
}

private val kotlin_language_version = System.getProperty("kotlin_language_version")?.let {
    "languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion('$it')"
}.orEmpty()

private val kotlin_api_version = System.getProperty("kotlin_api_version")?.let {
    "apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion('$it')"
}.orEmpty()

private val kotlin_warnings_settings = System.getProperty("kotlin_Werror_override").let {
    when (it) {
        "disable" -> ""
        else -> "allWarningsAsErrors = true"
    }
}

private val kotlin_additional_cli_options = System.getProperty("kotlin_additional_cli_options")?.let {
    val argsList = it.split(' ').map(String::trim).filter(String::isNotBlank)
    if (argsList.isEmpty()) {
        ""
    } else {
        argsList.joinToString(prefix = "\"", separator = "\", \"", postfix = "\"") { opt ->
            opt.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\r", "\\r")
                .replace("\"", "\\\"")
        }
    }
} ?: ""

private fun generateBuildScript(kotlinVersion: String, jvmToolchain: Int) =
    """
    buildscript {
        repositories {
            $kotlin_repo
            $plugin_repo_url
            mavenCentral()
        }
        dependencies {
            classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion'
            classpath 'org.jetbrains.kotlinx:kotlinx-benchmark-plugin:0.5.0-SNAPSHOT'
        }
    }
    
    apply plugin: 'kotlin-multiplatform'
    apply plugin: 'org.jetbrains.kotlinx.benchmark'
    
    repositories {
        $kotlin_repo
        $runtime_repo_url
        mavenCentral()
    }
    
    kotlin {
        jvmToolchain($jvmToolchain)

        sourceSets {
            commonMain {
                dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.5.0-SNAPSHOT")
                }
            }
        }
    }
    
    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask).configureEach {
        compilerOptions {
            $kotlin_language_version
            $kotlin_api_version

            progressiveMode = true
            $kotlin_warnings_settings
            $kotlin_additional_cli_options         
        }
    }
    """.trimIndent()
