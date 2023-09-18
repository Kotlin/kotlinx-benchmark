plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    infra {
        // According to https://kotlinlang.org/docs/native-target-support.html

        // Tier 1
        target("linuxX64")

        // Tier 2
        target("linuxArm64")

        // Tier 3
        target("androidNativeArm32")
        target("androidNativeArm64")
        target("androidNativeX86")
        target("androidNativeX64")
        target("mingwX64")

        common("darwin") {
            // Tier 1
            target("macosX64")
            target("macosArm64")
            target("iosSimulatorArm64")
            target("iosX64")

            // Tier 2
            target("watchosSimulatorArm64")
            target("watchosX64")
            target("watchosArm32")
            target("watchosArm64")
            target("tvosSimulatorArm64")
            target("tvosX64")
            target("tvosArm64")
            target("iosArm64")

            // Tier 3
            target("watchosDeviceArm64")
        }
    }

    jvm()
    js("jsIr", IR) { nodejs() }
    wasm("wasmJs") { d8() }

    sourceSets.all {
        val srcDirName = this.name

        kotlin.setSrcDirs(listOf("$srcDirName/src"))
        resources.setSrcDirs(listOf("$srcDirName/resources"))

        languageSettings.apply {
            progressiveMode = true
            optIn("kotlin.experimental.ExperimentalNativeApi")
            optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }

    sourceSets {
        getByName("commonTest") {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
        }
        getByName("jvmMain") {
            dependencies {
                compileOnly("org.openjdk.jmh:jmh-core:${property("jmhVersion")}")
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation("org.openjdk.jmh:jmh-core:${property("jmhVersion")}")
            }
        }

        val jsMain by creating
        
        getByName("jsIrMain") {
            dependsOn(jsMain)
        }
        getByName("nativeMain") {
            dependsOn(commonMain.get())
        }
    }
}

if (project.findProperty("publication_repository") == "space") {
    // publish to Space repository
    publishing {
        repositories {
            maven {
                name = "space"
                url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/dev")
                credentials {
                    username = project.findProperty("space.user") as String?
                    password = project.findProperty("space.token") as String?
                }
            }
        }
    }
}
