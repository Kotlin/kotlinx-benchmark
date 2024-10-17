/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.BuildTypeSettings
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

fun Project.additionalConfiguration() {
    knownBuilds.buildVersion.params {
        param(versionSuffixParameter, "")
    }
    knownBuilds.buildAll.features {
        commitStatusPublisher {
            vcsRootExtId = "${DslContext.settingsRoot.id}"
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = storedToken {
                    tokenId = "tc_token_id:CID_7db3007c46f7e30124f81ef54591b223:-1:4b5743ff-b95e-41b0-89d6-e9a50d3048db"
                }
            }
        }
    }
    platforms.forEach { platform ->
        knownBuilds.deployPublish.params {
            select("reverse.dep.*.system.publication_repository", "space", display = ParameterDisplay.PROMPT, label = "Publication Repository", options = listOf("space", "sonatype"))
        }
        knownBuilds.deployOn(platform).params {
            param("system.space.user", "abduqodiri.qurbonzoda")
            password("system.space.token", "credentialsJSON:7aa03210-1f86-452e-b786-920f8a321b7d")
        }
    }

    deployPlugin()
}

const val gradlePublishKey = "gradle.publish.key"
const val gradlePublishSecret = "gradle.publish.secret"

const val DEPLOY_PUBLISH_PLUGIN_ID = "Deploy_Publish_Plugin"

fun Project.deployPlugin() = BuildType {
    id(DEPLOY_PUBLISH_PLUGIN_ID)
    this.name = "Deploy (Publish Plugin)"
    commonConfigure()

    requirements {
        // Require Linux for publishPlugins
        contains("teamcity.agent.jvm.os.name", "Linux")
    }

    dependsOnSnapshot(this@deployPlugin.knownBuilds.buildAll)
    buildNumberPattern = this@deployPlugin.knownBuilds.buildVersion.depParamRefs.buildNumber.ref

    type = BuildTypeSettings.Type.DEPLOYMENT
    enablePersonalBuilds = false
    maxRunningBuilds = 1
    params {
        text(
            name = releaseVersionParameter,
            value = "",
            label = "Release Version",
            display = ParameterDisplay.PROMPT,
            allowEmpty = false
        )
    }

    steps {
        gradle {
            name = "Publish Plugin"
            jdkHome = "%env.$jdk%"
            jvmArgs = "-Xmx1g"
            gradleParams = "--info --stacktrace -P$releaseVersionParameter=%$releaseVersionParameter% -P$gradlePublishKey=%$gradlePublishKey% -P$gradlePublishSecret=%$gradlePublishSecret%"
            tasks = "clean :plugin:publishPlugins"
            buildFile = ""
            gradleWrapperPath = ""
        }
    }
}.also { buildType(it) }