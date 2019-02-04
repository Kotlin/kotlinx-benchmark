package org.jetbrains.gradle.benchmarks

internal fun ijLogStart(name: String, parent: String) = buildString {
    val shortName = name.substringAfterLast('.')
    val className = name.substringBeforeLast('.')
    append("<ijLog>")
    append("<event type='beforeTest'>")
    append("<test id='$name' parentId='$parent'>")
    append("<descriptor name='$shortName' className='$className' />")
    append("</test>")
    append("</event>")
    append("</ijLog>")
}

internal  fun ijLogFinish(name: String, parent: String) = buildString {
    append("<ijLog>")
    append("<event type='afterTest'>")
    append("<test id='$name' parentId='$parent'>")
    append("<result resultType='SUCCESS' startTime='0' endTime='0'/>")
    append("</test>")
    append("</event>")
    append("</ijLog>")
}

internal  fun ijLogOutput(name: String, parent: String, info: String) = buildString {
    append("<ijLog>")
    append("<event type='onOutput'>")
    append("<test id='$name' parentId='$parent'>")
    append("<event destination='StdOut'>")
    append("<![CDATA[${info.toByteArrayUtf8().encodeBase64()}]]>")
    append("</event>")
    append("</test>")
    append("</event>")
    append("</ijLog>")
}

private val BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
private val BASE64_MASK: Byte = 0x3f
private val BASE64_PAD = '='

private fun Int.toBase64(): Char = BASE64_ALPHABET[this]

private fun ByteArray.encodeBase64(): String {
    fun ByteArray.getOrZero(index: Int): Int = if (index >= size) 0 else get(index).toInt() and 0xFF

    val result = ArrayList<Char>(4 * size / 3)
    var index = 0
    while (index < size) {
        val symbolsLeft = size - index
        val padSize = if (symbolsLeft >= 3) 0 else (3 - symbolsLeft) * 8 / 6
        val chunk = (getOrZero(index) shl 16) or (getOrZero(index + 1) shl 8) or getOrZero(index + 2)
        index += 3

        for (i in 3 downTo padSize) {
            val char = (chunk shr (6 * i)) and BASE64_MASK.toInt()
            result.add(char.toBase64())
        }

        repeat(padSize) { result.add(BASE64_PAD) }
    }

    return String(result.toCharArray())
}

expect fun String.toByteArrayUtf8() : ByteArray