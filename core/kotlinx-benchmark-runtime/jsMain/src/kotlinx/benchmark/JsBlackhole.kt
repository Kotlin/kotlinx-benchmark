package kotlinx.benchmark

actual class Blackhole : CommonBlackhole()

actual fun Blackhole.flush() = flushMe()