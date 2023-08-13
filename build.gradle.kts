import kotlinx.team.infra.*

buildscript {
    val kotlin_version: String by project
    val infra_version: String by project

    repositories {
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/maven")
        gradlePluginPortal()

        addDevRepositoryIfEnabled(this, project)
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        classpath("kotlinx.team:kotlinx.team.infra:$infra_version")
    }
}

apply(plugin = "kotlinx.team.infra")

configure<InfraExtension> {
    teamcity {
        libraryStagingRepoDescription = project.name
    }

    publishing {
        include(":kotlinx-benchmark-runtime")

        libraryRepoUrl = "https://github.com/Kotlin/kotlinx-benchmark"

        if (project.findProperty("publication_repository") == "sonatype") {
            sonatype {}
        }
    }
}

// https://youtrack.jetbrains.com/issue/KT-48410
repositories {
    mavenCentral()
}

afterEvaluate {
    gradle.includedBuilds.forEach { included ->
        project(":kotlinx-benchmark-runtime").tasks.named("publishToMavenLocal").configure {
            dependsOn(included.task(":publishToMavenLocal"))
        }
    }
}

allprojects {
    val kotlin_version: String by project
    val infra_version: String by project

    logger.info("Using Kotlin $kotlin_version for project ${this.name}")
    repositories {
        addDevRepositoryIfEnabled(this, project)
    }
}
