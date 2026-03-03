pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "kotlinx-benchmark-plugin"

include(":klib-shim")
include(":klib-shim-2.3")
include(":klib-shim-2.4")
