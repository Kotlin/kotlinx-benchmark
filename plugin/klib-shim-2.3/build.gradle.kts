plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

kotlin {
    jvmToolchain(8)
}

group = "org.jetbrains.kotlinx"

dependencies {
    implementation(project(":klib-shim"))
    compileOnly(libs.kotlin.utilKlib)
    compileOnly("org.jetbrains.kotlinx:kotlinx-metadata-klib:0.0.6")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "kotlinx-benchmark-klib-shim-2.3"
        }
    }
}
