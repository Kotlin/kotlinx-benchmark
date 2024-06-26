import kotlinx.benchmarks_build.tasks.CheckReadmeTask

plugins {
    id("kotlinx.benchmarks_build.conventions.base")
}

//
//buildscript {
//    repositories {
//        maven { url 'https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/maven' }
//        gradlePluginPortal()
//
//        KotlinCommunity.addDevRepositoryIfEnabled(delegate, project)
//    }
//
////    dependencies {
////        classpath(libs.kotlinx.teamInfraGradlePlugin)
////    }
//}
//
//plugins {
//    id("base")
//    alias(libs.plugins.kotlin.multiplatform) apply false
//    alias(libs.plugins.kotlinx.binaryCompatibilityValidator)
//}
//
////apply plugin: 'kotlinx.team.infra'
//
////infra {
////    teamcity {
////        libraryStagingRepoDescription = project.name
////    }
////
////    publishing {
////        include(":kotlinx-benchmark-runtime")
////
////        libraryRepoUrl = "https://github.com/Kotlin/kotlinx-benchmark"
////
////        if (project.findProperty("publication_repository") == "sonatype") {
////            sonatype {}
////        }
////    }
////}
//
//// https://youtrack.jetbrains.com/issue/KT-48410
//repositories {
//    mavenCentral()
//}
//
//// region Workarounds for https://github.com/gradle/gradle/issues/22335
//tasks.register("apiDump") {
//    it.dependsOn(gradle.includedBuild("plugin").task(":apiDump"))
//}
//
//afterEvaluate {
//    gradle.includedBuilds.forEach { included ->
//        project(":kotlinx-benchmark-runtime").tasks.named("publishToMavenLocal") { dependsOn(included.task(":publishToMavenLocal")) }
//    }
//}
////endregion
//
//allprojects {
//    logger.info("Using Kotlin $kotlin_version for project $it")
//    repositories {
//        KotlinCommunity.addDevRepositoryIfEnabled(delegate, project)
//    }
//}
//
//apiValidation {
//    ignoredProjects += [
//            "examples",
//            "java",
//            "kotlin",
//            "kotlin-kts",
//            "kotlin-multiplatform",
//            "integration",
//    ]
//
//    nonPublicMarkers += ["kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi"]
//
//    klib {
//        it.enabled = true
//    }
//}

val checkReadme by tasks.registering(CheckReadmeTask::class) {
    minSupportedGradleVersion = libs.versions.minSupportedGradle
    readme = file("README.md")
}

tasks.check {
    dependsOn(checkReadme)
}
