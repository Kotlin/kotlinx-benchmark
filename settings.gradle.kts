pluginManagement {
    repositories {
        gradlePluginPortal()
        val kotlin_repo_url: String? by settings
        kotlin_repo_url?.takeIf { it.isNotEmpty() }?.let { repoUrl ->
            maven { url = uri(repoUrl) }
        }
    }
}

rootProject.name = "kotlinx-benchmark"

includeBuild("plugin")

include("runtime")
project(":runtime").name = "kotlinx-benchmark-runtime"

include("integration")

include("examples")
include("examples:kotlin-multiplatform")
include("examples:java")
include("examples:kotlin-kts")
