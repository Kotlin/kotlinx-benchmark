pluginManagement {
    repositories {
        gradlePluginPortal()
        val kotlinRepoUrl: String? by settings
        kotlinRepoUrl?.let { repoUrl ->
            if (repoUrl.isNotEmpty()) {
                maven { url = uri(repoUrl) }
            }
        }
    }
}

rootProject.name = "kotlinx-benchmark"

includeBuild("plugin")

include("runtime")
findProject(":runtime")?.name = "kotlinx-benchmark-runtime"

include("integration")

include("examples")
include("examples:kotlin-multiplatform")
include("examples:java")
include("examples:kotlin")
include("examples:kotlin-kts")
