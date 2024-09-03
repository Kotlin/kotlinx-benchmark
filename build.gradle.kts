import kotlinx.team.infra.InfraExtension
import kotlinx.validation.ExperimentalBCVApi
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapperKt
import tasks.CheckReadmeTask

buildscript {
    repositories {
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/maven")
        gradlePluginPortal()

        addDevRepositoryIfEnabled(this, project)
    }

    dependencies {
        classpath(libs.kotlinx.teamInfraGradlePlugin)

        String kotlinVersion = providers.gradleProperty("kotlin_version").orNull
        if (kotlinVersion != null && !kotlinVersion.isBlank()) {
            // In addition to overriding the Kotlin version in the Version Catalog,
            // also enforce the KGP version using a dependency constraint.
            // Constraints are stricter than Version Catalog.
            constraints {
                classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
            }
        }
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

tasks.register("apiCheck") {
    it.dependsOn(gradle.includedBuild("plugin").task(":apiCheck"))
}

afterEvaluate {
    gradle.includedBuilds.forEach { included ->
        project(":kotlinx-benchmark-runtime").tasks.named("publishToMavenLocal") { dependsOn(included.task(":publishToMavenLocal")) }
    }
}
tasks.register("publishToMavenLocal") {
    dependsOn(gradle.includedBuild("plugin").task(":publishToMavenLocal"))
}
//endregion

String currentKgpVersion = KotlinPluginWrapperKt.getKotlinPluginVersion(project)
logger.info("Using Kotlin Gradle Plugin ${currentKgpVersion}")

String kotlinVersionOverride = providers.gradleProperty("kotlin_version").getOrNull()

if (kotlinVersionOverride != null) {
    String versionCatalogKotlinVersion = libs.versions.kotlin.get()
    if (kotlinVersionOverride != versionCatalogKotlinVersion) {
        throw new IllegalStateException("Kotlin version in Version Catalog was not overridden. Expected:$kotlinVersionOverride, actual:$versionCatalogKotlinVersion.")
    }
    if (kotlinVersionOverride != currentKgpVersion) {
        throw new IllegalStateException("Kotlin Gradle Plugin version was not overridden. Expected:$kotlinVersionOverride, actual:$currentKgpVersion.")
    }
}

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
    dependsOn(tasks.named("apiCheck"))
}
