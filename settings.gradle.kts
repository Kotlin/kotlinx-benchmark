pluginManagement {
    repositories {
        gradlePluginPortal()
        val kotlinRepoUrl = providers.gradleProperty("kotlin_repo_url").orNull
        if (kotlinRepoUrl != null) {
            maven(kotlinRepoUrl)
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
include(":examples:kotlin")
include(":examples:kotlin-kts")
