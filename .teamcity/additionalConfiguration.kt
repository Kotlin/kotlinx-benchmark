/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.GradleBuildStep

fun Project.additionalConfiguration() {
    platforms.forEach { platform ->
        val gradleBuild = knownBuilds.buildOn(platform).steps.items.single() as GradleBuildStep
        gradleBuild.tasks += " " + fastBenchmarkTasks(platform)
    }
}

fun fastBenchmarkTasks(platform: Platform): String {
    return listOf(
        "js", "jvm", platform.nativeTaskPrefix()
    ).joinToString(separator = " ", transform = { "${it}FastBenchmark" })
}