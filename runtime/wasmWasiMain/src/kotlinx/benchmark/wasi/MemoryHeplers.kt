@file:OptIn(UnsafeWasmMemoryApi::class)

package kotlinx.benchmark.wasm.wasi

import kotlin.wasm.unsafe.*

internal class WasiError(val error: Any?)
    : Throwable(message = "WASI call failed with $error")

internal val String.size: Int
    get() = encodeToByteArray().size

internal const val PTR_SIZE = 4

// Returns size of byte array
internal fun readZeroTerminatedByteArray(ptr: Pointer, byteArray: ByteArray): Int {
    for (i in 0 until byteArray.size) {
        val b = (ptr + i).loadByte()
        if (b.toInt() == 0)
            return i
        byteArray[i] = b
    }
    error("Zero-terminated array is out of bounds")
}

internal fun MemoryAllocator.writeToLinearMemory(array: ByteArray): Pointer {
    val ptr = allocate(array.size)
    var currentPtr = ptr
    for (el in array) {
        currentPtr.storeByte(el)
        currentPtr += 1
    }
    return ptr
}

internal fun MemoryAllocator.writeToLinearMemory(array: __unsafe__IovecArray): Pointer {
    val ptr = allocate(array.size * 8)
    var currentPtr = ptr
    for (el in array) {
        __store___unsafe__Iovec(el, currentPtr)
        currentPtr += 8
    }
    return ptr
}

internal fun MemoryAllocator.writeToLinearMemory(array: __unsafe__CiovecArray): Pointer {
    val ptr = allocate(array.size * 8)
    var currentPtr = ptr
    for (el in array) {
        __store___unsafe__Ciovec(el, currentPtr)
        currentPtr += 8
    }
    return ptr
}

internal fun MemoryAllocator.writeToLinearMemory(string: String): Pointer =
    writeToLinearMemory(string.encodeToByteArray())

internal fun loadByteArray(addr: Pointer, size: Int): ByteArray =
    ByteArray(size) { i -> (addr + i).loadByte() }

internal fun loadString(addr: Pointer, size: Int): String {
    val bytes = loadByteArray(addr, size)
    val endIndex =
        if (size != 0 && bytes[size - 1] == 0.toByte())
            size - 1  // skip last 0 for 0-terminated strings
        else
            size

    return bytes.decodeToString(endIndex = endIndex)
}