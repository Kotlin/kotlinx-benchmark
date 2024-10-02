@file:JvmName("KotlinCommunity")

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.*
import java.net.*
import java.util.logging.Logger

/*
 * Functions in this file are responsible for configuring kotlinx-benchmarks build against a custom dev version
 * of Kotlin compiler.
 * Such configuration is used in aggregate builds of Kotlin in order to check whether not-yet-released changes
 * are compatible with our libraries (aka "integration testing that substitues lack of unit testing").
 */

/**
 * Kotlin compiler artifacts are expected to be downloaded from maven central by default.
 * In case of compiling with kotlin compiler artifacts that are not published into the MC,
 * a kotlin_repo_url gradle parameter should be specified.
 * To reproduce a build locally, a kotlin/dev repo should be passed.
 *
 * @return an url for a kotlin compiler repository parametrized from command line or gradle.properties,
 *   empty string otherwise
 */
fun getKotlinDevRepositoryUrl(project: Project): String? {
    val url = project.rootProject.properties["kotlin_repo_url"] as? String
    if (url != null) {
        project.logger.info("""Configured Kotlin Compiler repository url: '$url' for project ${project.name}""")
    }
    return url
}

/**
 * If the kotlin_repo_url gradle parameter is provided, adds it to the [repositoryHandler].
 */
fun addDevRepositoryIfEnabled(repositoryHandler: RepositoryHandler, project: Project) {
    val devRepoUrl = getKotlinDevRepositoryUrl(project) ?: return
    repositoryHandler.maven {
        url = URI.create(devRepoUrl)
    }
}

/**
 * Should be used for running against non-released Kotlin compiler on a system test level.
 *
 * @return a Kotlin API version parametrized from command line or gradle.properties, null otherwise
 */
fun getOverriddenKotlinNativeVersion(project: Project): String? {
    val nativeVersion = project.rootProject.properties["kotlin.native.version"] as? String
    return if (!nativeVersion.isNullOrBlank()) {
        project.logger.info("""Configured Kotlin Native distribution version: '$nativeVersion' for project $${project.name}""")
        nativeVersion
    } else {
        null
    }
}