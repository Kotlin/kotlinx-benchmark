kotlin {
    val target = when (System.getProperty("os.name")) {
        "Mac OS X" -> if (System.getProperty("os.arch") == "aarch64") macosArm64("native") else macosX64("native")
        "Linux" -> linuxX64("native")
        "Windows" -> mingwX64("native")
        else -> error("Unsupported OS")
    }

    target.apply {
        compilations.all {
            compilerOptions.options.apply {
                freeCompilerArgs.add("-Xallocator=custom")
            }
            cinterops {
                val nativeCinterop by creating {
                    defFile(project.file("src/nativeMain/kotlin/nativeCinterop.def"))
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.5.0-SNAPSHOT")
            }
        }
    }
}

benchmark {
    targets {
        register("native")
    }
}
