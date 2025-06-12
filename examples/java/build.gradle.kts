import kotlinx.benchmark.gradle.JvmBenchmarkTarget

plugins {
    java
    id("org.jetbrains.kotlinx.benchmark")
}

dependencies {
    implementation(project(":kotlinx-benchmark-runtime"))
}

benchmark {
    configurations {
        named("main") {
            iterationTime = 300
            iterationTimeUnit = "ms"
        }
        create("singleParam") {
            iterationTime = 300
            iterationTimeUnit = "ms"
            param("stringValue", "C", "D")
            param("intValue", 1, 10)
        }
    }
    targets {
        register("main") {
            this as JvmBenchmarkTarget
            jmhVersion = "1.37"
        }
    }
}
