pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
            val kotlinVersion = providers.gradleProperty("kotlin_version").orNull
            if (!kotlinVersion.isNullOrBlank()) {
                version("kotlin-for-gradle-plugin", kotlinVersion)
            }
        }
    }
}

rootProject.name = "kotlinx-benchmark-plugin"
