import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

plugins {
    id("kotlinx.benchmarks_build.conventions.base")
    `java-gradle-plugin`
    `embedded-kotlin`
    `java-test-fixtures`
    alias(libs.plugins.gradle.pluginPublish)
    id("kotlinx.benchmarks_build.conventions.publishing")
}

pluginBundle {
    website = "https://github.com/Kotlin/kotlinx-benchmark"
    vcsUrl = "https://github.com/Kotlin/kotlinx-benchmark.git"
    tags = listOf("benchmarking", "multiplatform", "kotlin")
}

gradlePlugin {
    plugins.register("kotlinBenchmark") {
        id = "org.jetbrains.kotlinx.benchmark"
        implementationClass = "kotlinx.benchmark.gradle.BenchmarksPlugin"
        displayName = "Gradle plugin for benchmarking"
        description = "Toolkit for running benchmarks for multiplatform Kotlin code."
    }
}

kotlin {
    compilerOptions {
        optIn.add("kotlinx.benchmark.gradle.internal.KotlinxBenchmarkPluginInternalApi")
    }
    target {
        compilations
            .matching { it.name == KotlinCompilation.MAIN_COMPILATION_NAME }
            .configureEach {
                compilerOptions.configure {
                    apiVersion.set(
                        // the version of Kotlin embedded in Gradle
                        libs.versions.minSupportedGradle.map {
                            if (GradleVersion.version(it) < GradleVersion.version("8.0")) {
                                @Suppress("DEPRECATION")
                                KotlinVersion.KOTLIN_1_4
                            } else {
                                KotlinVersion.KOTLIN_1_8
                            }
                        }
                    )
                }
            }
    }
}

dependencies {
    compileOnly(libs.kotlin.reflect)

    implementation(libs.squareup.kotlinpoet)

    implementation(libs.kotlin.utilKlibMetadata)
    implementation(libs.kotlin.utilKlib)
    implementation(libs.kotlin.utilIo)

    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlin.compilerEmbeddable)
    compileOnly(libs.jmh.generatorBytecode) // used in worker

    testFixturesApi(gradleTestKit())

    testImplementation(testFixtures(project))
    testImplementation(kotlin("test-junit"))

    devPublication(project)
    devPublication(project(":kotlinx-benchmark-runtime"))
//    devPublication(project(":kotlinx-benchmark-generator"))
}

val generatePluginConstants by tasks.registering {
    description = "Generates constants file used by BenchmarksPlugin"

    val outputDir = temporaryDir
    outputs.dir(outputDir).withPropertyName("outputDir")

    val constantsKtFile = outputDir.resolve("BenchmarksPluginConstants.kt")

    val benchmarkPluginVersion = project.providers.gradleProperty("releaseVersion")
        .orElse(project.version.toString())
    inputs.property("benchmarkPluginVersion", benchmarkPluginVersion)

    val minSupportedGradleVersion = libs.versions.minSupportedGradle
    inputs.property("minSupportedGradleVersion", minSupportedGradleVersion)

    doLast {
        constantsKtFile.writeText(
            """
            |package kotlinx.benchmark.gradle.internal
            |
            |internal object BenchmarksPluginConstants {
            |  const val BENCHMARK_PLUGIN_VERSION = "${benchmarkPluginVersion.get()}"
            |  const val MIN_SUPPORTED_GRADLE_VERSION = "${minSupportedGradleVersion.get()}"
            |}
            |
            """.trimMargin()
        )
    }
}

sourceSets {
    main {
        kotlin.srcDir(generatePluginConstants)
    }
}

tasks.test {
    devMavenPublish.configureTask(this)
//    systemProperty("plugin_repo_url", plugin.projectDir.resolve("build/maven").absoluteFile.invariantSeparatorsPath)
//    systemProperty("runtime_repo_url", rootProject.buildDir.resolve("maven").absoluteFile.invariantSeparatorsPath)
//    systemProperty("kotlin_repo_url", rootProject.properties["kotlin_repo_url"])
    systemProperty("kotlin_version", libs.versions.kotlin.get())
    systemProperty("minSupportedGradleVersion", libs.versions.minSupportedGradle.get())
}
