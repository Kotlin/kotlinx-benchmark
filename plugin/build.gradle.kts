import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import kotlinx.team.infra.InfraExtension
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

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
    //id("jar-file-attributes")
}

apply(plugin = "kotlinx.team.infra")

extensions.configure<InfraExtension> {
    publishing {
        include(":")

        libraryRepoUrl = "https://github.com/Kotlin/kotlinx-benchmark"
    }
}

logger.info("Using Kotlin ${libs.versions.kotlin.asProvider().get()} for project ${project.name}")

repositories {
    mavenCentral()
    gradlePluginPortal()

    val kotlinRepoUrl = providers.gradleProperty("kotlin_repo_url").orNull
    if (kotlinRepoUrl != null) {
        maven(kotlinRepoUrl)
    }
}

gradlePlugin {
    website = "https://github.com/Kotlin/kotlinx-benchmark"
    vcsUrl = "https://github.com/Kotlin/kotlinx-benchmark.git"

    plugins {
        register("benchmarkPlugin") {
            id = "org.jetbrains.kotlinx.benchmark"
            implementationClass = "kotlinx.benchmark.gradle.BenchmarksPlugin"
            displayName = "Gradle plugin for benchmarking"
            description = "Toolkit for running benchmarks for multiplatform Kotlin code."
            tags.set(listOf("benchmarking", "multiplatform", "kotlin"))
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

kotlin {
    jvmToolchain(8)

    @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
    compilerVersion = libs.versions.kotlin.`for`.gradle.plugin.get()

    compilerOptions {
        optIn.addAll(
                "kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi",
                "kotlin.RequiresOptIn",
        )
        /**
         * Those versions are configured according to https://docs.gradle.org/current/userguide/compatibility.html
         * and the Kotlin compiler compatibility policy stating that Kotlin 1.4 is compatible with 1.5 binaries
         */
        @Suppress("DEPRECATION", "DEPRECATION_ERROR")
        run {
            languageVersion = KotlinVersion.KOTLIN_1_5
            // The API available in the Kotlin stdlib embedded in the minimum supported Gradle.
            apiVersion = KotlinVersion.KOTLIN_1_4
        }
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

    val minSupportedKotlinVersion = libs.versions.minSupportedKotlin
    inputs.property("minSupportedKotlinVersion", minSupportedKotlinVersion)

    val kotlinCompilerVersion = libs.versions.kotlin.asProvider()
    inputs.property("kotlinCompilerVersion", kotlinCompilerVersion)

    val defaultJvmVersion = libs.versions.jmh
    inputs.property("defaultJmhVersion", defaultJvmVersion)

    doLast {
        constantsKtFile.writeText(
                """|package kotlinx.benchmark.gradle.internal
                |
                |internal object BenchmarksPluginConstants {
                |  const val BENCHMARK_PLUGIN_VERSION = "${benchmarkPluginVersion.get()}"
                |  const val MIN_SUPPORTED_GRADLE_VERSION = "${minSupportedGradleVersion.get()}"
                |  const val MIN_SUPPORTED_KOTLIN_VERSION = "${minSupportedKotlinVersion.get()}"
                |  const val DEFAULT_KOTLIN_COMPILER_VERSION = "${kotlinCompilerVersion.get()}"
                |  const val DEFAULT_JMH_VERSION = "${defaultJvmVersion.get()}"
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

// Both kotlinx.team.infra and Gradle publish plugins register their own javadocJar artifacts.
// We remove one of them here to avoid the collision leading to a build failure.
tasks.withType<AbstractPublishToMaven>().configureEach {
    doFirst {
        this as AbstractPublishToMaven
        val artifactsSet = publication.artifacts
        val javadocJars = artifactsSet.filter { it.classifier == "javadoc" }.toList()
        javadocJars.drop(1).forEach {
            artifactsSet.remove(it)
        }
    }
}

project.tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            "Implementation-Vendor" to "JetBrains",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
        )
    }
}

apiValidation {
    nonPublicMarkers += listOf("kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi")
}
