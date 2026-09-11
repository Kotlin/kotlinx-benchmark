package kotlinx.benchmark.tests

import kotlinx.benchmark.Blackhole

internal actual fun createTestBlackhole(): Blackhole =
    Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.")
