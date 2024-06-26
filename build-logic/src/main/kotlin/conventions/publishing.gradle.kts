package kotlinx.benchmarks_build.conventions

import java.net.URI

plugins {
    id("kotlinx.benchmarks_build.conventions.base")
    `maven-publish`
    signing
    id("kotlinx.benchmarks_build.dev_maven_publish.plugin")
}

fun Project.propertyOrEnv(name: String): String? =
    providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orNull

val stagingRepositoryId: String? = propertyOrEnv("libs.repository.id")
val sonatypeUsername: String? = propertyOrEnv("libs.sonatype.user")
val sonatypePassword: String? = propertyOrEnv("libs.sonatype.password")

val keyId: String? = project.propertyOrEnv("libs.sign.key.id")
val signingKey: String? = project.propertyOrEnv("libs.sign.key.private")
val signingKeyPassphrase: String? = project.propertyOrEnv("libs.sign.passphrase")

fun sonatypeRepositoryUri(): URI {
    val repositoryId: String? = stagingRepositoryId
    return when {
        repositoryId.isNullOrEmpty() ->
            error("Staging repository id 'libs.repository.id' is not specified.")

        repositoryId == "auto" -> {
            // Using implicitly created staging, for MPP it's likely a mistake
            logger.warn("INFRA: using an implicitly created staging for ${project.rootProject.name}")
            URI("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
        }

        else -> {
            URI("https://oss.sonatype.org/service/local/staging/deployByRepositoryId/$repositoryId")
        }
    }
}

val javadocJar by tasks.registering(Jar::class) {
    description = "Intentionally empty Javadoc JAR, required by Maven Central"
    archiveClassifier.set("javadoc")
}

publishing {
    repositories {
        maven(sonatypeRepositoryUri()) {
            name = "sonatype"
            credentials {
                username = sonatypeUsername
                password = sonatypePassword?.trim()
            }
        }
    }


    publications.withType<MavenPublication>().configureEach {
        artifact(javadocJar)
        pom {
            description.convention(artifactId)
            url = "https://github.com/Kotlin/kotlinx-benchmark"
            licenses {
                license {
                    name = "The Apache License, Version 2.0"
                    url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                }
            }
            scm {
                url = "https://github.com/Kotlin/kotlinx-benchmark"
            }
            developers {
                developer {
                    name = "JetBrains Team"
                    organization = "JetBrains"
                    organizationUrl = "https://www.jetbrains.com"
                }
            }
        }
    }
}

if (keyId != null) {
    signing {
        useInMemoryPgpKeys(keyId, signingKey, signingKeyPassphrase)
        val signingTasks = sign(publishing.publications)
        // due to each publication including the same javadoc artifact file,
        // every publication signing task produces (overwrites) the same javadoc.asc signature file beside
        // and includes it to that publication
        // Thus, every publication publishing task implicitly depends on every signing task
        tasks.withType<AbstractPublishToMaven>().configureEach {
            dependsOn(signingTasks) // make this dependency explicit
        }
    }
} else {
    logger.warn("INFRA: signing key id is not specified, artifact signing is not enabled.")
}
