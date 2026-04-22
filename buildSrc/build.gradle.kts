import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(17)
    compilerOptions.jvmTarget = JvmTarget.JVM_1_8
}
tasks.withType(JavaCompile::class) {
    options.release = 8
}
