package kotlinx.benchmark

public actual typealias Blackhole = org.openjdk.jmh.infra.Blackhole


internal actual fun Blackhole.flush() = Unit
