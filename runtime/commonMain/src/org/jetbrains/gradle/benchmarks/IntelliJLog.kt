package org.jetbrains.gradle.benchmarks

internal fun ijSuiteStart(parent: String, id: String) = buildString {
    append("<ijLog>")
    if (parent.isEmpty()) // TODO: bug or inconsistency, if two suites are nested, IDEA won't group them
        append("<event type='beforeSuite'>")
    else
        append("<event type='beforeTest'>")
    append("<test id='$id' parentId='$parent'>")
    append("<descriptor name='$id'/>")
    append("</test>")
    append("</event>")
    append("</ijLog>")
}

internal fun ijSuiteFinish(parent: String, id: String, status: BenchmarkReporter.FinishStatus) = buildString {
    append("<ijLog>")
    if (parent.isEmpty()) // TODO: bug or inconsistency, if two suites are nested, IDEA won't group them
        append("<event type='afterSuite'>")
    else
        append("<event type='afterTest'>")
    append("<test id='$id' parentId='$parent'>")
    append("<result resultType='${status.toString().toUpperCase()}' startTime='0' endTime='0'/>")
    append("</test>")
    append("</event>")
    append("</ijLog>")
}

internal fun ijBenchmarkStart(parent: String, className: String, methodName: String) = buildString {
    append("<ijLog>")
    append("<event type='beforeTest'>")
    append("<test id='$className.$methodName' parentId='$parent'>")
    append("<descriptor name='$methodName' className='$className' />")
    append("</test>")
    append("</event>")
    append("</ijLog>")
}

internal fun ijBenchmarkFinish(parent: String, id: String, status: BenchmarkReporter.FinishStatus) = buildString {
    append("<ijLog>")
    append("<event type='afterTest'>")
    append("<test id='$id' parentId='$parent'>")
    append("<result resultType='${status.toString().toUpperCase()}' startTime='0' endTime='0'/>")
    append("</test>")
    append("</event>")
    append("</ijLog>")
}

internal fun ijBenchmarkFinishException(parent: String, id: String, error: String, stacktrace: String) = buildString {
    append("<ijLog>")
    append("<event type='afterTest'>")
    append("<test id='$id' parentId='$parent'>")
    append("<result resultType='FAILURE' startTime='0' endTime='0'>")
    append("<errorMsg>")
    append("<![CDATA[${error.toByteArrayUtf8().encodeBase64()}]]>")
    append("</errorMsg>")
    append("<stackTrace>")
    append("<![CDATA[${stacktrace.toByteArrayUtf8().encodeBase64()}]]>")
    append("</stackTrace>")
    append("</result>")
    append("</test>")
    append("</event>")
    append("</ijLog>")
}

internal fun ijLogOutput(parent: String, id: String, info: String) = buildString {
    append("<ijLog>")
    append("<event type='onOutput'>")
    append("<test id='$id' parentId='$parent'>")
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

expect fun String.toByteArrayUtf8(): ByteArray