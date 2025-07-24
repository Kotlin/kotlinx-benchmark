plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

evaluationDependsOn(":kotlinx-benchmark-runtime")

val runtime get() = project(":kotlinx-benchmark-runtime")
val plugin get() = gradle.includedBuild("plugin")

dependencies {
    implementation(gradleTestKit())

    testImplementation(kotlin("test-junit"))
}

tasks.test {
    dependsOn(plugin.task(":publishToBuildLocal"))
    dependsOn(runtime.tasks.getByName("publishToBuildLocal"))

    systemProperty("plugin_repo_url", plugin.projectDir.resolve("build/maven").absoluteFile.invariantSeparatorsPath)
    systemProperty("runtime_repo_url", rootProject.buildDir.resolve("maven").absoluteFile.invariantSeparatorsPath)
    getKotlinDevRepositoryUrl(project)?.let {
        systemProperty("kotlin_repo_url", it)
    }
    systemProperty("kotlin_version", libs.versions.kotlin.asProvider().get())
    getOverriddenKotlinLanguageVersion(project)?.let {
        systemProperty("kotlin_language_version", it)
    }
    getOverriddenKotlinApiVersion(project)?.let {
        systemProperty("kotlin_api_version", it)
    }
    getOverriddenKotlinNativeVersion(project)?.let {
        systemProperty("kotlin.native.version", it)
    }
    systemProperty("minSupportedGradleVersion", libs.versions.minSupportedGradle.get())
    systemProperty("minSupportedKotlinVersion", libs.versions.minSupportedKotlin.get())
    systemProperty("kotlin_Werror_override", if (getAllWarningsAsErrorsValue(project)) "enable" else "disable")
    project.providers.gradleProperty("kotlin_additional_cli_options").orNull?.let {
        systemProperty("kotlin_additional_cli_options", it)
    }
    
    val forks = project.providers.gradleProperty("testing.max.forks").orNull?.toInt()
        ?: (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)

    maxParallelForks = forks
}
