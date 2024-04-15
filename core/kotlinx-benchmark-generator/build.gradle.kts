plugins {
    kotlin("jvm")
}

description = "Generates benchmark test KMP code"

// TODO KT-66949 Currently this project is empty while we restructure the project.
//      As part of KT-66949 the generator code will be extracted from the Gradle Plugin into this subproject.

repositories {
    mavenCentral()
}
