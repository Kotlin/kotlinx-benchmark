package org.jetbrains.gradle.benchmarks.tests

import org.jetbrains.gradle.benchmarks.*
import kotlin.test.*


class FormatTests {
    @Test
    fun formatPrecision() {
        assertEquals("1,200", 1200.0.formatAtMost(4))
        assertEquals("12.03", 12.03.formatAtMost(4))
        assertEquals("1,200", 1200.00005.formatAtMost(4))
        assertEquals("0.00000005000", 0.00000005.formatAtMost(4))
        assertEquals("0.0001001", 0.00010005.formatAtMost(4))
    }
    
    @Test
    fun formatTimeUnits() {
        assertEquals("83.33 ops/us", 12.0.nanosToText(Mode.Throughput, BenchmarkTimeUnit.MICROSECONDS))
        assertEquals("0.01200 us/op", 12.0.nanosToText(Mode.AverageTime, BenchmarkTimeUnit.MICROSECONDS))
    }
}