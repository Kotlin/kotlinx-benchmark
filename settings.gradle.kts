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
            val kotlinVersion = providers.gradleProperty("kotlin_version").orNull
            if (!kotlinVersion.isNullOrBlank()) {
                // Override the default Kotlin version.
                // The only intended use-case is for testing dev Kotlin builds using kotlinx-benchmark.
                // The Kotlin version should not be overridden during regular development.
                version("kotlin", kotlinVersion)
            }
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
include(":examples:kotlin-kts")
