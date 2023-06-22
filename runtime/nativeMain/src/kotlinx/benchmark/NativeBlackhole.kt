package kotlinx.benchmark

import kotlinx.cinterop.objcPtr
import kotlin.random.Random

actual class Blackhole {
    /*
     * To make sure that dead-code elimination will not remove computations whose
     * result passed to consume-methods, compare the value with two other values
     * and store it to a special object only if the value is simultaneously equal to both
     * values loaded from fields. At the same time, fields are initialized so that their values
     * are always different, thus the store will never happen.
     *
     * One of the possible issues is that the compiler may figure out that fields never change
     * its values and initialized with different values, so the test described above should always fail.
     */
    private var intA: Int = Random.nextInt()
    private var intB: Int = intA + 1
    private var charA: Char = Random.nextInt().toChar()
    private var charB: Char = charA + 1
    private var longA: Long = Random.nextLong()
    private var longB: Long = longA + 1
    private var byteA: Byte = Random.nextInt().toByte()
    private var byteB: Byte = (byteA + 1).toByte()
    private var shortA: Short = Random.nextInt().toShort()
    private var shortB: Short = (shortA + 1).toShort()
    private var boolA: Boolean = Random.nextBoolean()
    private var boolB: Boolean = !boolA
    private var floatA: Float = Random.nextFloat()
    private var floatB: Float = floatA + 1.0f
    private var doubleA: Double = Random.nextDouble()
    private var doubleB: Double = doubleA + 1.0
    private var ptrA: Long = Random.nextLong()
    private var ptrB: Long = ptrA + 8
    private var dump: Dump? = Dump()

    class Dump {
        var byteValue: Byte = 0
        var shortValue: Short = 0
        var intValue: Int = 0
        var longValue: Long = 0
        var charValue: Char = '0'
        var boolValue: Boolean = false
        var floatValue: Float = 0.0f
        var doubleValue: Double = 0.0
        var anyValuePtr: Long = 0
    }

    actual fun consume(obj: Any?) {
        val ptr = obj.objcPtr().toLong()
        if (ptrA == ptr && ptrB == ptr) {
            dump!!.anyValuePtr = ptr
        }
    }
    actual fun consume(bool: Boolean) {
        if (boolA == bool && boolB == bool) {
            dump!!.boolValue = bool
        }
    }
    actual fun consume(c: Char) {
       if (charA == c && charB == c) {
           dump!!.charValue = c
       }
    }
    actual fun consume(b: Byte) {
        if (byteA == b && byteB == b) {
            dump!!.byteValue = b
        }
    }
    actual fun consume(s: Short) {
        if (shortA == s && shortB == s) {
            dump!!.shortValue = s
        }
    }
    actual fun consume(i: Int) {
        if (intA == i && intB == i) {
            dump!!.intValue = i
        }
    }
    actual fun consume(l: Long) {
        if (longA == l && longB == l) {
            dump!!.longValue = l
        }
    }
    actual fun consume(f: Float) {
        if (floatA == f && floatB == f) {
            dump!!.floatValue = f
        }
    }
    actual fun consume(d: Double) {
        if (doubleA == d && doubleB == d) {
            dump!!.doubleValue = d
        }
    }
}

actual fun Blackhole.flush() = Unit