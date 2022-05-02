package kotlinx.benchmark.gradle

import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.OutputStream

internal class ConsoleAndFilesOutputStream : OutputStream() {
    private val buffer = ByteArrayOutputStream()
    private var currentStream: OutputStream = System.out
    private val fileTag = "<FILE:"
    private val endFileTag = "<ENDFILE>"
    private val openTag = '<'.toInt()
    private val closeTag = '>'.toInt()
    private var tagOpened = false

    private fun processTag(tag: String) {
        if (tag.startsWith(fileTag)) {
            check(currentStream !is FileOutputStream) { "$endFileTag not found" }
            val fileName = tag.substring(fileTag.length, tag.lastIndex)
            currentStream = FileOutputStream(fileName)
        } else if (tag == endFileTag) {
            val currentFile = currentStream as? FileOutputStream
            check(currentFile != null) { "$fileTag not found" }
            currentFile.flush()
            currentFile.close()
            currentStream = System.out
        } else {
            buffer.writeTo(currentStream)
        }
        buffer.reset()
    }

    override fun write(b: Int) {
        when (b) {
            openTag -> {
                buffer.writeTo(currentStream)
                buffer.reset()
                buffer.write(b)
                tagOpened = true
            }
            closeTag -> {
                if (tagOpened) {
                    buffer.write(b)
                    processTag(buffer.toString())
                    tagOpened = false
                } else {
                    buffer.write(b)
                }
            }
            else -> {
                buffer.write(b)
            }
        }
    }

    override fun flush() {
        buffer.flush()
        buffer.writeTo(currentStream)
        buffer.reset()
        currentStream.flush()
    }

    override fun close() {
        flush()
        buffer.close()
        currentStream.close()
    }
}