import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

buildscript {
    ext.kotlinDevUrl = rootProject.properties["kotlin_repo_url"]
    repositories {
        maven { url 'https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/maven' }
        mavenCentral()
        if (kotlinDevUrl != null) {
            maven { url = kotlinDevUrl }
        }
    }

    dependencies {
        classpath(libs.kotlinx.teamInfraGradlePlugin)
    }
}

plugins {
    id 'java-gradle-plugin'
    id 'maven-publish'
    alias(libs.plugins.gradle.pluginPublish)
    alias(libs.plugins.kotlinx.binaryCompatibilityValidator)
    alias(libs.plugins.kotlin.jvm)
}

apply(plugin: 'kotlinx.team.infra')

infra {
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

    if (kotlinDevUrl != null) {
        maven { url = kotlinDevUrl }
    }
}

pluginBundle {
    website = 'https://github.com/Kotlin/kotlinx-benchmark'
    vcsUrl = 'https://github.com/Kotlin/kotlinx-benchmark.git'
    tags = ['benchmarking', 'multiplatform', 'kotlin']
}

gradlePlugin {
    plugins {
        benchmarkPlugin {
            id = "org.jetbrains.kotlinx.benchmark"
            implementationClass = "kotlinx.benchmark.gradle.BenchmarksPlugin"
            displayName = "Gradle plugin for benchmarking"
            description = "Toolkit for running benchmarks for multiplatform Kotlin code."
        }
    }
}

sourceSets {
    main {
        kotlin.srcDirs = ['main/src']
        java.srcDirs = ['main/src']
        resources.srcDirs = ['main/resources']
    }
    test {
        kotlin.srcDirs = ['test/src']
        java.srcDirs = ['test/src']
        resources.srcDirs = ['test/resources']
    }
}

tasks.named("compileKotlin", KotlinCompilationTask.class) {
    compilerOptions {
        optIn.addAll(
                "kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi",
                "kotlin.RequiresOptIn",
        )
        //noinspection GrDeprecatedAPIUsage
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

def generatePluginConstants = tasks.register("generatePluginConstants") {
    description = "Generates constants file used by BenchmarksPlugin"

    File outputDir = temporaryDir
    outputs.dir(outputDir).withPropertyName("outputDir")

    File constantsKtFile = new File(outputDir, "BenchmarksPluginConstants.kt")

    Provider<String> benchmarkPluginVersion = project.providers.gradleProperty("releaseVersion")
            .orElse(project.version.toString())
    inputs.property("benchmarkPluginVersion", benchmarkPluginVersion)

    Provider<String> minSupportedGradleVersion = libs.versions.minSupportedGradle
    inputs.property("minSupportedGradleVersion", minSupportedGradleVersion)

    doLast {
        constantsKtFile.write(
                """|package kotlinx.benchmark.gradle.internal
                |
                |internal object BenchmarksPluginConstants {
                |  const val BENCHMARK_PLUGIN_VERSION = "${benchmarkPluginVersion.get()}"
                |  const val MIN_SUPPORTED_GRADLE_VERSION = "${minSupportedGradleVersion.get()}"
                |}
                |""".stripMargin()
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
                url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/dev"
                credentials {
                    username = project.findProperty("space.user")
                    password = project.findProperty("space.token")
                }
            }
        }
    }
}

apiValidation {
    nonPublicMarkers += ["kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi"]
}
