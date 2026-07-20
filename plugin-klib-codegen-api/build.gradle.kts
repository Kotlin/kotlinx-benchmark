import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    java
    `maven-publish`
    id("org.jetbrains.kotlin.jvm")
}

java {
    withSourcesJar()
}

publishing {
    publications.register("default", MavenPublication::class.java) {
        from(components["java"])
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

kotlin {
    jvmToolchain(17)

    // Module is intended as the API dependency for the plugin
    // and it compiles with exactly the same settings.
    @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
    compilerVersion = libs.versions.kotlin.`for`.gradle.plugin.get()
    coreLibrariesVersion = "1.8.0"

    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8

        /**
         * Gradle 8.0 embeds Kotlin 1.8.x, so the plugin can be built against Kotlin language and API 1.8
         * while remaining compatible with the minimum supported Gradle version.
         */
        @Suppress("DEPRECATION", "DEPRECATION_ERROR")
        run {
            languageVersion = KotlinVersion.KOTLIN_1_8
            apiVersion = KotlinVersion.KOTLIN_1_8
        }
    }

    tasks.withType(JavaCompile::class) {
        targetCompatibility = "8"
        sourceCompatibility = "8"
    }
}

dependencies {
    implementation(libs.kotlin.compilerEmbeddable)
    implementation(libs.squareup.kotlinpoet)
}

tasks.test {
    useJUnitPlatform()
}
