plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven { setUrl("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev") }
    mavenLocal()
}

evaluationDependsOn(":kotlinx-benchmark-runtime")

val runtime get() = project(":kotlinx-benchmark-runtime")
val plugin get() = gradle.includedBuild("plugin")

dependencies {
    implementation(gradleTestKit())

    testImplementation(kotlin("test-junit"))
}

tasks.test {
    dependsOn(plugin.task(":publishToBuildLocal"))
    dependsOn(runtime.tasks.getByName("publishToBuildLocal"))

    systemProperty("plugin_repo_url", plugin.projectDir.resolve("build/maven").absoluteFile.invariantSeparatorsPath)
    systemProperty("runtime_repo_url", rootProject.buildDir.resolve("maven").absoluteFile.invariantSeparatorsPath)
    rootProject.properties["kotlin_repo_url"]?.let { systemProperty("kotlin_repo_url", it) }
    systemProperty("kotlin_version", rootProject.properties["kotlin_version"]!!)
}

allprojects {
    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
        kotlinOptions {
            languageVersion = rootProject.properties["kotlin_language_version"].toString()
            apiVersion = rootProject.properties["kotlin_api_version"].toString()
        }
    }
}

tasks.getByName("apiCheck").dependsOn(rootProject.gradle.includedBuild("plugin").task(":apiCheck"))