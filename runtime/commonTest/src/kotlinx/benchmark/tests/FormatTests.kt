package kotlinx.benchmark.tests

import kotlinx.benchmark.*
import kotlin.test.*


class FormatTests {
    @Test
    fun format() {
        assertEquals("1,200.0000", 1200.0.format(4))
        assertEquals("12,000.0000", 12000.0.format(4))
        assertEquals("1,200,000.0000", 1200000.0.format(4))
        assertEquals("12.0300", 12.03.format(4))
        assertEquals("1,200.0001", 1200.00005.format(4))
        assertEquals("0.0000", 0.00000005.format(4))
        assertEquals("0.0001", 0.00010005.format(4))
    }

    @Test
    fun formatGrouping() {
        assertEquals("1200.0000", 1200.0.format(4, useGrouping = false))
        assertEquals("12000.0000", 12000.0.format(4, useGrouping = false))
        assertEquals("1200000.0000", 1200000.0.format(4, useGrouping = false))
        assertEquals("12.0300", 12.03.format(4, useGrouping = false))
        assertEquals("1200.0001", 1200.00005.format(4, useGrouping = false))
        assertEquals("0.0000", 0.00000005.format(4, useGrouping = false))
        assertEquals("0.0001", 0.00010005.format(4, useGrouping = false))
    }

    @Test
    fun formatSignificant() {
        assertEquals("1,200", 1200.0.formatSignificant(4))
        assertEquals("12.03", 12.03.formatSignificant(4))
        assertEquals("1,200", 1200.00005.formatSignificant(4))
        assertEquals("0.00000005000", 0.00000005.formatSignificant(4))
        assertEquals("0.0001001", 0.00010005.formatSignificant(4))
    }

    @Test
    fun nanosToText() {
        assertEquals("83.3333 ops/us", 12.0.nanosToText(Mode.Throughput, BenchmarkTimeUnit.MICROSECONDS))
        assertEquals("0.0120000 us/op", 12.0.nanosToText(Mode.AverageTime, BenchmarkTimeUnit.MICROSECONDS))
    }
}
