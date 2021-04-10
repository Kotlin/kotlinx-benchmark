package kotlinx.benchmark

import kotlinx.cinterop.pin
import kotlin.experimental.xor
import kotlin.math.ulp
import kotlin.native.ref.WeakReference
import kotlin.random.Random
import kotlin.system.getTimeNanos

actual class Blackhole {
    actual fun consume(obj: Any?) {
        obj?.pin()
    }
    actual fun consume(bool: Boolean) {
        bool.pin()
    }
    actual fun consume(c: Char) {
        c.pin()
    }
    actual fun consume(b: Byte) {
        b.pin()
    }
    actual fun consume(s: Short) {
        s.pin()
    }
    actual fun consume(i: Int) {
        i.pin()
    }
    actual fun consume(l: Long) {
        l.pin()
    }
    actual fun consume(f: Float) {
        f.pin()
    }
    actual fun consume(d: Double) {
        d.pin()
    }
}

// Alternative implementation for Blackhole (same as JMH).
// Too heavy for Native runtime in 10 times slower than Blackhole implemented via native methods.
/*abstract class BaseBlackhole {
    var b1: Byte
    var bool1: Boolean
    var c1: Char
    var s1: Short
    var i1: Int
    var l1: Long
    var f1: Float
    var d1: Double
    var b2: Byte
    var bool2: Boolean
    var c2: Char
    var s2: Short
    var i2: Int
    var l2: Long
    var f2: Float
    var d2: Double
    var obj1: Any?
    var nullBait: BaseBlackhole? = null
    var tlr: Int
    var tlrMask: Int

    init {
        val r = Random(getTimeNanos())
        tlr = r.nextInt()
        tlrMask = 1
        obj1 = Any()
        b1 = r.nextInt().toByte()
        b2 = (b1 + 1).toByte()
        bool1 = r.nextBoolean()
        bool2 = !bool1
        c1 = r.nextInt().toChar()
        c2 = (c1.toInt() + 1).toChar()
        s1 = r.nextInt().toShort()
        s2 = (s1 + 1).toShort()
        i1 = r.nextInt()
        i2 = i1 + 1
        l1 = r.nextLong()
        l2 = l1 + 1
        f1 = r.nextFloat()
        f2 = f1 + f1.ulp
        d1 = r.nextDouble()
        d2 = d1 + d1.ulp
        check(b1 != b2) { "byte tombstones are equal" }
        check(bool1 != bool2) { "boolean tombstones are equal" }
        check(c1 != c2) { "char tombstones are equal" }
        check(s1 != s2) { "short tombstones are equal" }
        check(i1 != i2) { "int tombstones are equal" }
        check(l1 != l2) { "long tombstones are equal" }
        check(f1 != f2) { "float tombstones are equal" }
        check(d1 != d2) { "double tombstones are equal" }
    }
}

actual class Blackhole: BaseBlackhole() {
    actual fun consume(obj: Any?) {
        var tlrMask: Int = this.tlrMask

        val tlr: Int = (this.tlr * 1664525 + 1013904223).also { this.tlr = it }
        if (tlr and tlrMask == 0) {
            // SHOULD ALMOST NEVER HAPPEN IN MEASUREMENT
            this.obj1 = obj?.let { WeakReference<Any>(it) }
            tlrMask = (tlrMask shl 1) + 1
        }
    }
    actual fun consume(bool: Boolean) {
        val bool1 = bool1

        val bool2 = bool2
        if (bool xor bool1 == bool xor bool2) {
            // SHOULD NEVER HAPPEN
            nullBait!!.bool1 = bool
        }
    }
    actual fun consume(c: Char) {
        val c1 = c1

        val c2 = c2
        if (c.toInt() xor c1.toInt() === c.toInt() xor c2.toInt()) {
            // SHOULD NEVER HAPPEN
            nullBait!!.c1 = c
        }
    }
    actual fun consume(b: Byte) {
        val b1 = b1

        val b2 = b2
        if (b xor b1 === b xor b2) {
            // SHOULD NEVER HAPPEN
            nullBait!!.b1 = b
        }
    }
    actual fun consume(s: Short) {
        val s1 = s1

        val s2 = s2
        if (s xor s1 === s xor s2) {
            // SHOULD NEVER HAPPEN
            nullBait!!.s1 = s
        }
    }
    actual fun consume(i: Int) {
        val i1 = i1

        val i2 = i2
        if (i xor i1 === i xor i2) {
            // SHOULD NEVER HAPPEN
            nullBait!!.i1 = i
        }
    }
    actual fun consume(l: Long) {
        val l1 = l1

        val l2 = l2
        if (l xor l1 === l xor l2) {
            // SHOULD NEVER HAPPEN
            nullBait!!.l1 = l
        }
    }
    actual fun consume(f: Float) {
        val f1 = f1

        val f2 = f2
        if ((f === f1) and (f === f2)) {
            // SHOULD NEVER HAPPEN
            nullBait!!.f1 = f
    }
    actual fun consume(d: Double) {
        val d1 = d1 // volatile read

        val d2 = d2
        if ((d === d1) and (d === d2)) {
            // SHOULD NEVER HAPPEN
            nullBait!!.d1 = d
        }
    }
}*/