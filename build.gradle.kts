import kotlinx.team.infra.InfraExtension
import kotlinx.validation.ExperimentalBCVApi
import tasks.CheckReadmeTask

buildscript {
    repositories {
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/maven")
        gradlePluginPortal()

        addDevRepositoryIfEnabled(this, project)
    }

    dependencies {
        classpath(libs.kotlinx.teamInfraGradlePlugin)
    }
}

plugins {
    id("base")
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlinx.binaryCompatibilityValidator)
}

apply(plugin = "kotlinx.team.infra")

extensions.configure<InfraExtension> {
    teamcity {
        libraryStagingRepoDescription = project.name
    }

    publishing {
        include(":kotlinx-benchmark-runtime")

        libraryRepoUrl = "https://github.com/Kotlin/kotlinx-benchmark"

        if (project.findProperty("publication_repository") == "sonatype") {
            sonatype {}
        }
    }
}

// https://youtrack.jetbrains.com/issue/KT-48410
repositories {
    mavenCentral()
}

// region Workarounds for https://github.com/gradle/gradle/issues/22335
tasks.register("apiDump") {
    dependsOn(gradle.includedBuild("plugin").task(":apiDump"))
}
tasks.register("publishToMavenLocal") {
    dependsOn(gradle.includedBuild("plugin").task(":publishToMavenLocal"))
}
//endregion

allprojects {
    repositories {
        addDevRepositoryIfEnabled(this, project)
    }
}

apiValidation {
    ignoredProjects += listOf(
            "examples",
            "java",
            "kotlin",
            "kotlin-kts",
            "kotlin-multiplatform",
            "integration",
    )

    nonPublicMarkers += listOf("kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi")

    @OptIn(ExperimentalBCVApi::class)
    klib {
        enabled = true
    }
}

val checkReadme by tasks.registering(CheckReadmeTask::class) {
    minSupportedGradleVersion = libs.versions.minSupportedGradle
    readme = file("README.md")
}

tasks.check {
    dependsOn(checkReadme)
}
