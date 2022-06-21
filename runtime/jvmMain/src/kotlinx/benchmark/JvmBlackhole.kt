package kotlinx.benchmark

actual typealias Blackhole = org.openjdk.jmh.infra.Blackhole

actual fun Blackhole.flush() = Unit
