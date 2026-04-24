package kotlinx.benchmark

internal abstract class OutputStream  {
    internal val buffer: StringBuilder = StringBuilder(4096)
    abstract fun flush()
    abstract fun write(b: Char)
}

internal object ConsoleLinesStream : OutputStream() {
    override fun flush() {
        println(buffer.toString())
        buffer.clear()
    }

    override fun write(b: Char) {
        if (b == '\n') {
            flush()
        } else {
            buffer.append(b)
        }
    }
}

private fun appendToFile(filePath: String, textToAppend: String): Unit =
    js("require('fs').appendFileSync(filePath, textToAppend, 'utf8')")

internal class FileStream(val filename: String) : OutputStream() {
    override fun flush() {
        appendToFile(filename, buffer.toString())
        buffer.clear()
    }

    override fun write(b: Char) {
        buffer.append(b)
    }
}

internal class ConsoleAndFilesOutputStream : OutputStream() {
    private var currentStream: OutputStream = ConsoleLinesStream
    private val fileTag = "<FILE:"
    private val endFileTag = "<ENDFILE>"
    private val openTag = '<'
    private val closeTag = '>'
    private var tagOpened = false

    private fun writeToCurrentStream() {
        buffer.toString().forEach(currentStream::write)
    }

    private fun processTag(tag: String) {
        if (tag.startsWith(fileTag)) {
            check(currentStream !is FileStream) { "$endFileTag not found" }
            val fileName = tag.substring(fileTag.length, tag.lastIndex)
            println(fileName)
            currentStream = FileStream(fileName)
        } else if (tag == endFileTag) {
            val currentFile = currentStream as? FileStream
            check(currentFile != null) { "$fileTag not found" }
            currentFile.flush()
            currentStream = ConsoleLinesStream
        } else {
            writeToCurrentStream()
        }
        buffer.clear()
    }

    override fun write(b: Char) {
        when (b) {
            openTag -> {
                writeToCurrentStream()
                buffer.clear()
                buffer.append(b)
                tagOpened = true
            }
            closeTag -> {
                if (tagOpened) {
                    buffer.append(b)
                    processTag(buffer.toString())
                    tagOpened = false
                } else {
                    buffer.append(b)
                }
            }
            else -> {
                buffer.append(b)
            }
        }
    }

    override fun flush() {
        writeToCurrentStream()
        buffer.clear()
        currentStream.flush()
    }
}