package kotlinx.benchmark.integration

class ProjectBuilder {
    private val configurations = mutableMapOf<String, BenchmarkConfiguration>()

    private val benchmarkLibraryVersion = "0.5.0-SNAPSHOT"
    // The version should be set to be compatible with the Gradle version used by kotlinx-benchmarks.
    // See https://developer.android.com/build/releases/about-agp#updating-gradle
    private val agpMultiplatformVersion = "8.6.1"
    var kotlinVersion: String = System.getProperty("kotlin_version")
    var jvmToolchain: Int = 11 // TODO JDK_11_DISCUSS

    fun configuration(name: String, configuration: BenchmarkConfiguration.() -> Unit = {}) {
        configurations[name] = BenchmarkConfiguration().apply(configuration)
    }

    fun generateSettingsScripts(original: String, androidSupport: Boolean): String {
        val pluginManagement = """
        pluginManagement {
            repositories {
                $kotlin_repo
                $plugin_repo_url
                mavenCentral()
                gradlePluginPortal()
                ${if (androidSupport) "google()" else ""}
            }
        }                    
        """.trimIndent()
        val dependencyManagement = """
        dependencyResolutionManagement {
            repositories {
                $kotlin_repo
                $runtime_repo_url
                mavenCentral()
                ${if (androidSupport) "google()" else ""}
            }
        }
        """.trimIndent()
        return pluginManagement + "\n\n" + original + "\n\n" + dependencyManagement
    }

    fun generateBuildScript(original: String, androidSupport: Boolean): String {

        val script =
            """
benchmark {
    configurations {
        ${configurations.flatMap { it.value.lines(it.key) }.joinToString("\n        ")}
    }
}
            """.trimIndent()

        return generateBuildScript(
            kotlinVersion = kotlinVersion,
            jvmToolchain = jvmToolchain,
            agpMultiplatformVersion = if (androidSupport) agpMultiplatformVersion else null,
            benchmarkLibraryVersion = benchmarkLibraryVersion
        ) + "\n\n" + original + "\n\n" + script
    }
}

private val kotlin_repo = System.getProperty("kotlin_repo_url")?.let {
    "maven { url '${it.replace("\\","\\\\")}' }"
}.orEmpty()

private val plugin_repo_url = System.getProperty("plugin_repo_url")!!.let {
    "maven { url '${it.replace("\\","\\\\")}' }"
}

private val runtime_repo_url = System.getProperty("runtime_repo_url")!!.let {
    "maven { url '${it.replace("\\","\\\\")}' }"
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

private fun generateBuildScript(
    kotlinVersion: String,
    jvmToolchain: Int,
    agpMultiplatformVersion: String?,
    benchmarkLibraryVersion: String
) =
    """
    plugins {
        id 'org.jetbrains.kotlin.multiplatform' version '$kotlinVersion'
        ${
            when (agpMultiplatformVersion != null) {
                true -> "id 'com.android.kotlin.multiplatform.library' version '$agpMultiplatformVersion'"
                else -> ""
            }
        }
        id 'org.jetbrains.kotlinx.benchmark' version '$benchmarkLibraryVersion'
    }
    
    kotlin {
        jvmToolchain($jvmToolchain)

        sourceSets {
            commonMain {
                dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:${benchmarkLibraryVersion}")
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
