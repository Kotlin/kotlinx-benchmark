/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.*
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
                    tokenId = "tc_token_id:CID_7db3007c46f7e30124f81ef54591b223:-1:05a5bd7b-50f1-4d04-b542-b9283f6b0148"
                }
            }
        }
    }

    val deployment = knownBuilds.deploymentSubproject
    val deployStart = deployment.knownBuilds.deployStart
    deployStart.params.param("reverse.dep.*.DeploymentName", "kotlinx.collections.immutable %releaseVersion%")

    val deployPlugin = deployment.deployPlugin(deployment.knownBuilds.deployVersion, false, DEPLOY_PUBLISH_PLUGIN_ID, "Deploy (Publish Plugin)")
    val testDeployPlugin = deployment.deployPlugin(deployment.knownBuilds.deployVersion, true, VALIDATE_PUBLISH_PLUGIN_ID, "Validate (Publish Plugin)")
    deployPlugin.dependsOnSnapshot(testDeployPlugin)
    deployStart.dependsOnSnapshot(deployPlugin)
    deployStart.dependsOnSnapshot(testDeployPlugin)

    // Check with Kotlin master only on Linux
    buildWithKotlinMaster(Platform.Linux, knownBuilds.buildVersion).also {
        knownBuilds.buildAll.dependsOnSnapshot(it, onFailure = FailureAction.ADD_PROBLEM)
    }
}

const val gradlePublishKey = "gradle.publish.key"
const val gradlePublishSecret = "gradle.publish.secret"

const val DEPLOY_PUBLISH_PLUGIN_ID = "Deploy_Publish_Plugin"
const val VALIDATE_PUBLISH_PLUGIN_ID = "Validate_Plugin_Publication"

const val BUILD_WITH_KOTLIN_MASTER_ID = "Build_with_Kotlin_Master_Linux"

fun Project.deployPlugin(deployVersion: BuildType, validateOnly: Boolean, taskId: String, taskName: String) = BuildType {
    id(taskId)
    this.name = taskName
    commonConfigure()

    if (!validateOnly) {
        features {
            approval {
                approvalRules = "user:filipp.zhinkin"
                manualRunsApproved = false
            }
        }
    }

    requirements {
        // Require Linux for publishPlugins
        contains("teamcity.agent.jvm.os.name", "Linux")
    }

    // dependsOnSnapshot(this@deployPlugin.knownBuilds.buildAll)
    dependsOnSnapshot(deployVersion)
    buildNumberPattern = deployVersion.depParamRefs.buildNumber.ref

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
            gradleParams = "--info --stacktrace -P$releaseVersionParameter=%$releaseVersionParameter% " +
                    "-P$gradlePublishKey=%$gradlePublishKey% -P$gradlePublishSecret=%$gradlePublishSecret%"
            tasks = "clean :plugin:publishPlugins" + (if (validateOnly) " --validate-only" else "")
            buildFile = ""
            gradleWrapperPath = ""
        }
    }
}.also { buildType(it) }

fun Project.buildWithKotlinMaster(platform: Platform, versionBuild: BuildType) = BuildType {
    id(BUILD_WITH_KOTLIN_MASTER_ID)
    this.name = "Build with Kotlin Master (${platform.buildTypeName()})"

    requirements {
        contains("teamcity.agent.jvm.os.name", platform.teamcityAgentName())
    }
    commonConfigure()

    dependsOnSnapshot(versionBuild)
    params {
        param(versionSuffixParameter, versionBuild.depParamRefs[versionSuffixParameter].ref)
        param(teamcitySuffixParameter, versionBuild.depParamRefs[teamcitySuffixParameter].ref)
    }

    val kotlinVersionParameter = "dep.Kotlin_KotlinPublic_BuildNumber.deployVersion"

    dependsOn(AbsoluteId("Kotlin_KotlinPublic_Artifacts")) {
        artifacts {
            buildRule = lastSuccessful()
            cleanDestination = true
            artifactRules = "+:maven.zip!**=>artifacts/kotlin"
        }
    }

    steps {
        gradle {
            name = "Build and Test ${platform.buildTypeName()} Binaries"
            jdkHome = "%env.$jdk%"
            jvmArgs = "-Xmx1g"
            tasks = "clean publishToBuildLocal check"
            // --continue is needed to run tests for all targets even if one target fails
            gradleParams = listOf(
                "-x kotlinStoreYarnLock",
                "--info", "--stacktrace", "--continue",
                "-P$versionSuffixParameter=%$versionSuffixParameter%", "-P$teamcitySuffixParameter=%$teamcitySuffixParameter%",
                "-Pkotlin_repo_url=file://%teamcity.build.checkoutDir%/artifacts/kotlin",
                "-Pkotlin_version=%$kotlinVersionParameter%", "-Pkotlin.native.version=%$kotlinVersionParameter%",
                "-Pmin_supported_gradle_version=8.0",
                "-Pkotlin.native.enableKlibsCrossCompilation=false"
            ).joinToString(separator = " ")
            buildFile = ""
            gradleWrapperPath = ""
        }
    }

    // What files to publish as build artifacts
    artifactRules = "+:build/maven=>maven\n+:build/api=>api\n+:artifacts"
}.also { buildType(it) }
