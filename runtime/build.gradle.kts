import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import java.util.*

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(8)

    // According to https://kotlinlang.org/docs/native-target-support.html

    // Tier 1
    linuxX64()
    macosX64()
    macosArm64()
    iosSimulatorArm64()
    iosX64()

    // Tier 2
    linuxArm64()
    watchosSimulatorArm64()
    watchosX64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosX64()
    tvosArm64()
    iosArm64()

    // Tier 3
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    mingwX64()
    watchosDeviceArm64()

    jvm()
    js { nodejs() }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
        d8()
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("jsWasmJsShared") {
                withJs()
                withWasmJs()
            }
        }
    }

    targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                    optIn.addAll(
                        "kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi",
                        "kotlin.RequiresOptIn",
                    )
                }
            }

            // If a compiler version is below 2.2.20, ExperimentalWasmJsInterop may not be resolved.
            if (kotlin.isCompilerVersionAtLeast(2, 2, 20)) {
                if (target.platformType == KotlinPlatformType.wasm) {
                    compileTaskProvider.configure {
                        compilerOptions.optIn.add("kotlin.js.ExperimentalWasmJsInterop")
                    }
                }
            }
        }
    }

    sourceSets.configureEach {
        kotlin.srcDirs(listOf("$name/src"))
        resources.srcDirs(listOf("$name/resources"))
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
        }
        jvmMain {
            dependencies {
                compileOnly(libs.jmh.core)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.jmh.core)
            }
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
                    username = project.findProperty("space.user") as? String?
                    password = project.findProperty("space.token") as? String?
                }
            }
        }
    }
}

// Workaround for TeamCity build failure:
// Task 'compileTestKotlinLinuxX64' uses this output of task 'signLinuxX64Publication' without declaring an explicit or implicit dependency.
// TODO: Find out and fix the issue
tasks.withType(KotlinNativeCompile::class).matching { it.name.lowercase(Locale.ROOT).contains("test") }.configureEach {
    dependsOn(tasks.withType(Sign::class))
}

tasks.withType(KotlinNativeCompile::class).configureEach {
    compilerOptions.freeCompilerArgs.addAll(
        "-opt-in=kotlin.experimental.ExperimentalNativeApi",
        "-opt-in=kotlin.native.runtime.NativeRuntimeApi",
        "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
    )
}

@OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
fun KotlinMultiplatformExtension.isCompilerVersionAtLeast(major: Int, minor: Int, patch: Int): Boolean {
    val version = compilerVersion.orNull ?: return false
    val mainVersion = version.split('-')[0]
    val parts = mainVersion.split('.')
    if (parts.size != 3) return false
    val actualMajor = parts[0].toIntOrNull() ?: return false
    val actualMinor = parts[1].toIntOrNull() ?: return false
    val actualPatch = parts[2].toIntOrNull() ?: return false
    return actualMajor > major ||
            (actualMajor == major && actualMinor > minor) ||
            (actualMajor == major && actualMinor == minor && actualPatch >= patch)
}
