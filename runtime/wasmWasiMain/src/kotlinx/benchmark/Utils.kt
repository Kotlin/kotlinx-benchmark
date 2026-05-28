@file:OptIn(UnsafeWasmMemoryApi::class)

package kotlinx.benchmark

import kotlin.math.pow
import kotlin.math.round
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi

internal actual fun Double.format(precision: Int, useGrouping: Boolean): String {
    val factor = 10.0.pow(precision)
    val roundedValue = round(this * factor) / factor
    val plainDouble = roundedValue.toPlainString()

    val integerPart = plainDouble.substringBefore('.')
    val groupedIntegerPart = if (useGrouping) {
        integerPart.reversed().chunked(3).joinToString(",").reversed()
    } else {
        integerPart
    }

    if (precision == 0) return groupedIntegerPart

    val fractionalPart = plainDouble.substringAfter('.')
    val paddedFraction = fractionalPart.padEnd(precision, '0')

    return "$groupedIntegerPart.$paddedFraction"
}

private fun Double.toPlainString(): String {
    val text = this.toString().lowercase()
    if ('e' !in text) return text

    val sign = if (text.startsWith("-")) "-" else ""
    val unsigned = text.removePrefix("-")

    val mantissa = unsigned.substringBefore("e")
    val exponent = unsigned.substringAfter("e").toInt()

    val integerPart = mantissa.substringBefore('.')
    val fractionPart = mantissa.substringAfter('.', "")

    val digits = "$integerPart$fractionPart"

    val decimalPlaces = fractionPart.length
    val shift = exponent - decimalPlaces

    if (shift >= 0) {
        return "$sign$digits${"0".repeat(shift)}"
    }

    val pointIndex = digits.length + shift
    return if (pointIndex > 0) {
        "$sign${digits.substring(0, pointIndex)}.${digits.substring(pointIndex)}"
    } else {
        "${sign}0.${"0".repeat(-pointIndex)}$digits"
    }
}