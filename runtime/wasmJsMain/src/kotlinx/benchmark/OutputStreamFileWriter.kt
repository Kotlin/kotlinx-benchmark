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
    fs.appendFileSync(filePath, textToAppend, "utf8")

internal class FileStream(val filename: String) : OutputStream() {
    override fun flush() {
        appendToFile(filename, buffer.toString())
        buffer.clear()
    }

    override fun write(b: Char) {
        buffer.append(b)
    }
}

internal object Fd3Stream : OutputStream() {
    override fun flush() {
        fs.writeSync(3, buffer.toString())
        buffer.clear()
    }

    override fun write(b: Char) {
        buffer.append(b)
    }
}

internal class SplittedOutputStream(private val processResultTags: Boolean) : OutputStream() {
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
            currentStream = FileStream(fileName)
        } else if (tag == endFileTag) {
            check(currentStream is FileStream) { "$fileTag not found" }
            currentStream.flush()
            currentStream = ConsoleLinesStream
        } else if (processResultTags && tag == resultTag) {
            currentStream = Fd3Stream
        } else if (processResultTags && tag == endResultTag) {
            check(currentStream is Fd3Stream) { "$resultTag not found" }
            currentStream.flush()
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