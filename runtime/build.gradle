import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

repositories {
    mavenCentral()
}

kotlin {
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
    js("jsIr", IR) { nodejs() }
    wasm("wasmJs") { d8() }

    applyDefaultHierarchyTemplate { root ->
        root.common { common ->
            common.group("jsWasmJsShared") { group ->
                group.withJs()
                group.withWasm()
            }
        }
    }

    targets.configureEach {
        compilations.configureEach {
            compilerOptions.options.with {
                allWarningsAsErrors.set(true)
                freeCompilerArgs.add("-Xexpect-actual-classes")
                optIn.addAll(
                        "kotlinx.benchmark.internal.KotlinxBenchmarkRuntimeInternalApi",
                        "kotlin.RequiresOptIn",
                )
            }
        }
    }

    sourceSets.configureEach {
        kotlin.srcDirs = ["$it.name/src"]
        resources.srcDirs = ["$it.name/resources"]
        languageSettings {
            progressiveMode = true
        }
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation 'org.jetbrains.kotlin:kotlin-test'
            }
        }
        jvmMain {
            dependsOn(commonMain)
            dependencies {
                compileOnly(libs.jmh.core)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.jmh.core)
            }
        }
        jsMain {
            dependsOn(commonMain)
            jsIrMain.dependsOn(it)
        }
        nativeMain {
            dependsOn(commonMain)
        }
    }
}

if (project.findProperty("publication_repository") == "space") {
    // publish to Space repository
    publishing {
        repositories {
            maven {
                name = "space"
                url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/dev"
                credentials {
                    username = project.findProperty("space.user")
                    password = project.findProperty("space.token")
                }
            }
        }
    }
}

// Workaround for TeamCity build failure:
// Task 'compileTestKotlinLinuxX64' uses this output of task 'signLinuxX64Publication' without declaring an explicit or implicit dependency.
// TODO: Find out and fix the issue
tasks.withType(KotlinNativeCompile).matching { it.name.toLowerCase().contains("test") }.configureEach {
    it.dependsOn(tasks.withType(Sign))
}

tasks.withType(KotlinNativeCompile).configureEach {
    compilerOptions.freeCompilerArgs.addAll(
            "-opt-in=kotlin.experimental.ExperimentalNativeApi",
            "-opt-in=kotlin.native.runtime.NativeRuntimeApi",
            "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
    )
}
