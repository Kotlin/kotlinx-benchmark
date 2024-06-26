rootProject.name = "kotlinx-benchmark"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
//        if (settings.hasProperty("kotlin_repo_url") && settings.kotlin_repo_url != null) {
//            maven { url = settings.kotlin_repo_url }
//        }
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        mavenCentral()
    }
}


includeBuild("kotlinx-benchmark-core")
includeBuild("kotlinx-benchmark-examples")
