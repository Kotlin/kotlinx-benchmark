import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import kotlinx.team.infra.InfraExtension

buildscript {
    repositories {
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/maven")
        mavenCentral()
        val kotlinRepoUrl = providers.gradleProperty("kotlin_repo_url").orNull
        if (kotlinRepoUrl != null) {
            maven(kotlinRepoUrl)
        }
    }

    dependencies {
        classpath(libs.kotlinx.teamInfraGradlePlugin)
        // Note: unlike the root project, don't override KGP version in this project.
        // Gradle plugins should only use the embedded-kotlin version.
        // kotlinx-benchmark uses an external KGP the moment... but that should be fixed
        // https://github.com/Kotlin/kotlinx-benchmark/issues/244
    }
}

plugins {
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.gradle.pluginPublish)
    alias(libs.plugins.kotlinx.binaryCompatibilityValidator)
    alias(libs.plugins.kotlin.jvm)
}

apply(plugin = "kotlinx.team.infra")

extensions.configure<InfraExtension> {
    teamcity {
        libraryStagingRepoDescription = project.name
    }
    publishing {
        include(":")

        libraryRepoUrl = "https://github.com/Kotlin/kotlinx-benchmark"
    }
}

logger.info("Using Kotlin ${libs.versions.kotlin.get()} for project ${project.name}")

repositories {
    mavenCentral()
    gradlePluginPortal()

    val kotlinRepoUrl = providers.gradleProperty("kotlin_repo_url").orNull
    if (kotlinRepoUrl != null) {
        maven(kotlinRepoUrl)
    }
}

pluginBundle {
    website = "https://github.com/Kotlin/kotlinx-benchmark"
    vcsUrl = "https://github.com/Kotlin/kotlinx-benchmark.git"
    tags = listOf("benchmarking", "multiplatform", "kotlin")
}

gradlePlugin {
    plugins {
        register("benchmarkPlugin") {
            id = "org.jetbrains.kotlinx.benchmark"
            implementationClass = "kotlinx.benchmark.gradle.BenchmarksPlugin"
            displayName = "Gradle plugin for benchmarking"
            description = "Toolkit for running benchmarks for multiplatform Kotlin code."
        }
    }
}

sourceSets {
    main {
        kotlin.srcDirs(listOf("main/src"))
        java.srcDirs(listOf("main/src"))
        resources.srcDirs(listOf("main/resources"))
    }
    test {
        kotlin.srcDirs("test/src")
        java.srcDirs("test/src")
        resources.srcDirs("test/resources")
    }
}

tasks.compileKotlin {
    compilerOptions {
        optIn.addAll(
                "kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi",
                "kotlin.RequiresOptIn",
        )
        @Suppress("DEPRECATION")
        apiVersion = KotlinVersion.KOTLIN_1_4 // the version of Kotlin embedded in Gradle
    }
}

dependencies {
    implementation(libs.kotlin.reflect)

    implementation(libs.squareup.kotlinpoet)

    implementation(libs.kotlin.utilKlibMetadata)
    implementation(libs.kotlin.utilKlib)
    implementation(libs.kotlin.utilIo)

    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlin.compilerEmbeddable)
    compileOnly(libs.jmh.generatorBytecode) // used in worker
}

val generatePluginConstants by tasks.registering {
    description = "Generates constants file used by BenchmarksPlugin"

    val outputDir = temporaryDir
    outputs.dir(outputDir).withPropertyName("outputDir")

    val constantsKtFile = File(outputDir, "BenchmarksPluginConstants.kt")

    val benchmarkPluginVersion = project.providers.gradleProperty("releaseVersion")
            .orElse(project.version.toString())
    inputs.property("benchmarkPluginVersion", benchmarkPluginVersion)

    val minSupportedGradleVersion = libs.versions.minSupportedGradle
    inputs.property("minSupportedGradleVersion", minSupportedGradleVersion)

    Provider<String> kotlinCompilerVersion = libs.versions.kotlin
    inputs.property("kotlinCompilerVersion", kotlinCompilerVersion)

    doLast {
        constantsKtFile.writeText(
                """|package kotlinx.benchmark.gradle.internal
                |
                |internal object BenchmarksPluginConstants {
                |  const val BENCHMARK_PLUGIN_VERSION = "${benchmarkPluginVersion.get()}"
                |  const val MIN_SUPPORTED_GRADLE_VERSION = "${minSupportedGradleVersion.get()}"
                |  const val DEFAULT_KOTLIN_COMPILER_VERSION = "${kotlinCompilerVersion.get()}"
                |}
                |""".trimMargin()
        )
    }
}

sourceSets {
    main {
        kotlin.srcDir(generatePluginConstants)
    }
}

if (project.findProperty("publication_repository") == "space") {
    // publish to Space repository
    publishing {
        repositories {
            maven {
                name = "space"
                url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/dev")
                credentials {
                    username = project.findProperty("space.user") as? String?
                    password = project.findProperty("space.token") as? String?
                }
            }
        }
    }
}

apiValidation {
    nonPublicMarkers += listOf("kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi")
}
