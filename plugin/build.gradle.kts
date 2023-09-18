import kotlinx.team.infra.*

val jmhVersion: String by project
val kotlin_version: String by project

buildscript {
    val kotlin_version: String by project
    val infra_version: String by project
    
    repositories {
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/maven")
        mavenCentral()
        rootProject.properties["kotlin_repo_url"]?.let {
            maven { url = uri(it.toString()) }
        }
    }
    dependencies {
        classpath("kotlinx.team:kotlinx.team.infra:$infra_version")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    }
}

plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.21.0"
    kotlin("jvm") version "1.9.0"
}

apply(plugin = "kotlinx.team.infra")

configure<InfraExtension> {
    teamcity {
        libraryStagingRepoDescription = project.name
    }

    publishing {
        include(":")

        libraryRepoUrl = "https://github.com/Kotlin/kotlinx-benchmark"
    }
}

logger.info("Using Kotlin ${kotlin_version} for project ${project.name}")

repositories {
    mavenCentral()
    gradlePluginPortal()

    rootProject.properties["kotlin_repo_url"]?.let { url ->
        maven { setUrl(url.toString()) }
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
        kotlin.srcDir("main/src")
        java.srcDir("main/src")
        resources.srcDir("main/resources")
    }
    test {
        kotlin.srcDir("test/src")
        java.srcDir("test/src")
        resources.srcDir("test/resources")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
        apiVersion = "1.4"
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    implementation("com.squareup:kotlinpoet:1.3.0")
    implementation("org.jetbrains.kotlin:kotlin-util-klib-metadata:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-util-klib:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-util-io:$kotlin_version")
    compileOnly("org.jetbrains.kotlin.multiplatform:org.jetbrains.kotlin.multiplatform.gradle.plugin:$kotlin_version")
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlin_version")
    compileOnly("org.openjdk.jmh:jmh-generator-bytecode:$jmhVersion")
}

tasks.register("overridePluginVersion") {
    description = "Overrides BenchmarksPlugin.PLUGIN_VERSION during release builds"
    onlyIf {
        project.findProperty("releaseVersion") != null
    }
    doLast {
        val benchmarksPluginFile = "${projectDir}/main/src/kotlinx/benchmark/gradle/BenchmarksPlugin.kt"
        val releaseVersion = project.findProperty("releaseVersion") as String?
        ant.withGroovyBuilder {
            "replaceregexp"(
                "file" to benchmarksPluginFile,
                "match" to """const val PLUGIN_VERSION = "[\d.]+-SNAPSHOT"""",
                "replace" to """const val PLUGIN_VERSION = "$releaseVersion"""",
                "encoding" to "UTF-8"
            )
        }
    }
}

tasks.named("compileKotlin").configure {
    dependsOn("overridePluginVersion")
}

if (project.findProperty("publication_repository") == "space") {
    // publish to Space repository
    publishing {
        repositories {
            maven {
                name = "space"
                url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/dev")
                credentials {
                    username = project.findProperty("space.user") as String?
                    password = project.findProperty("space.token") as String?
                }
            }
        }
    }
}