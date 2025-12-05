import kotlinx.benchmark.gradle.*

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlinx.benchmark")
}

sourceSets.configureEach {
    java.setSrcDirs(listOf("$name/src"))
    resources.setSrcDirs(listOf("$name/resources"))
}
dependencies {
    implementation(project(":kotlinx-benchmark-runtime"))
}

kotlin {
    jvmToolchain(8)
}

benchmark {
    configurations {
        named("main") {
            iterationTime = 5
            iterationTimeUnit = "sec"
            
        }
    }
    targets {
        register("main")
    }
}
