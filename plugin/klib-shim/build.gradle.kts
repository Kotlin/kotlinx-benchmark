plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

kotlin {
    jvmToolchain(8)
}

group = "org.jetbrains.kotlinx"

dependencies {
    compileOnly(libs.kotlin.utilKlib)
    compileOnly(libs.kotlinx.metadata.klib)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "kotlinx-benchmark-klib-shim"
        }
    }
}
