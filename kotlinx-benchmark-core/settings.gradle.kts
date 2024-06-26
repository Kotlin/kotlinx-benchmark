rootProject.name = "kotlinx-benchmark-core"

pluginManagement {
    includeBuild("../build-logic")
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
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }

    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        mavenCentral()
        google()

        //region workaround for https://youtrack.jetbrains.com/issue/KT-51379
        // TODO remove when updating to Kotlin 2.0
        ivy("https://download.jetbrains.com/kotlin/native/builds") {
            name = "KotlinNative"
            patternLayout {
                listOf(
                    "macos-x86_64",
                    "macos-aarch64",
                    "osx-x86_64",
                    "osx-aarch64",
                    "linux-x86_64",
                    "windows-x86_64",
                ).forEach { os ->
                    listOf("dev", "releases").forEach { stage ->
                        artifact("$stage/[revision]/$os/[artifact]-[revision].[ext]")
                    }
                }
            }
            content { includeModuleByRegex(".*", ".*kotlin-native-prebuilt.*") }
            metadataSources { artifact() }
        }
        //endregion

        //region Declare the Node.js & Yarn download repositories
        // Workaround https://youtrack.jetbrains.com/issue/KT-68533/
        ivy("https://cache-redirector.jetbrains.com/nodejs.org/dist/") {
            name = "Node Distributions at $url"
            patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("org.nodejs", "node") }
        }
        ivy("https://github.com/yarnpkg/yarn/releases/download") {
            name = "Yarn Distributions at $url"
            patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.yarnpkg", "yarn") }
        }
        //endregion

        ivy("https://storage.googleapis.com/chromium-v8/official/canary") {
            name = "ChromiumV8OfficialCanary"
            patternLayout {
                artifact("[artifact]-[revision].[ext]")
            }
            metadataSources { artifact() }
            content { includeModule("google.d8", "v8") }
        }
    }
}

include(":kotlinx-benchmark-generator")
include(":kotlinx-benchmark-runtime")
include(":kotlinx-benchmark-plugin")
