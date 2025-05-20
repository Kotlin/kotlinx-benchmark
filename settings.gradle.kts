pluginManagement {
    repositories {
        gradlePluginPortal()
        val kotlinRepoUrl = providers.gradleProperty("kotlin_repo_url").orNull
        if (kotlinRepoUrl != null) {
            maven(kotlinRepoUrl)
        }
    }
}


dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            listOf(
                "kotlin" to "kotlin_version",
                "minSupportedGradle" to "min_supported_gradle_version",
            ).forEach { (versionName, propertyName) ->
                val overrideVersion = providers.gradleProperty(propertyName).orNull
                if (!overrideVersion.isNullOrBlank()) {
                    // Override the default version.
                    // The only intended use-case is for testing dev Kotlin builds using kotlinx-benchmark.
                    // These versions should not be overridden during regular development.
                    version(versionName, overrideVersion)
                }
            }
        }
    }
}

plugins {
    id("com.gradle.develocity") version "3.18"
}


develocity {
    server = "https://ge.jetbrains.com/"
    buildScan {
        capture {
            fileFingerprints = true
            buildLogging = true
            uploadInBackground = true
        }
        obfuscation {
            ipAddresses { _ -> listOf("0.0.0.0") }
            hostname { _ -> "concealed" }
            username { _ -> "TeamCity" }
        }
    }
}

rootProject.name = "kotlinx-benchmark"

includeBuild("plugin")

include(":runtime")
project(":runtime").name = "kotlinx-benchmark-runtime"

include(":integration")

include(":examples")
include(":examples:kotlin-multiplatform")
include(":examples:java")
include(":examples:kotlin-jvm-separate-benchmark-source-set")
include(":examples:kotlin-jvm")
