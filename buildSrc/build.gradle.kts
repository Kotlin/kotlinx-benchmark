import org.jetbrains.kotlin.gradle.plugin.*
import java.util.*

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    maven { setUrl("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev") }
    mavenLocal()
}
