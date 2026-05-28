package kotlinx.benchmark

import kotlinx.benchmark.wasm.wasi.Fd
import kotlinx.benchmark.wasm.wasi.OFLAGS
import kotlinx.benchmark.wasm.wasi.RIGHTS
import kotlinx.benchmark.wasm.wasi.fd_close
import kotlinx.benchmark.wasm.wasi.fd_filestat_get
import kotlinx.benchmark.wasm.wasi.getTimeNanoseconds
import kotlinx.benchmark.wasm.wasi.path_open
import kotlinx.benchmark.wasm.wasi.size
import kotlinx.benchmark.wasm.wasi.wasiFileRead
import kotlinx.benchmark.wasm.wasi.wasiFileWrite
import kotlinx.benchmark.wasm.wasi.wasiGetArguments
import kotlinx.benchmark.wasm.wasi.wasiPrestatDirectoryName

private fun String.findPathDescriptor(): Fd {
    var descriptor = 3
    while (!startsWith(wasiPrestatDirectoryName(descriptor), ignoreCase = true)) {
        descriptor++
    }
    return descriptor
}

private fun String.getRelativePathFor(descriptor: Fd): String {
    val descriptorPath = wasiPrestatDirectoryName(descriptor)
    return this.substring(descriptorPath.size)
}

private object WasiEngineSupport : BenchmarkEngineSupport() {
    override fun readFile(path: String): String {
        val pathDescriptor = path.findPathDescriptor()
        val relativePath = path.getRelativePathFor(pathDescriptor)

        val fileDescriptor = path_open(
            fd = pathDescriptor,
            dirflags = 0,
            relativePath,
            0,
            RIGHTS.FD_READ or RIGHTS.FD_FILESTAT_GET,
            0,
            0,
        )

        val stat = fd_filestat_get(fileDescriptor)
        val fileSize = stat.size.toInt()
        val buffer = ByteArray(fileSize)
        val read = wasiFileRead(
            fd = fileDescriptor,
            iovs = listOf(buffer),
        )

        check(read == fileSize)

        fd_close(fileDescriptor)

        return buffer.decodeToString()
    }

    override fun writeFile(path: String, content: String) {
        val pathDescriptor = path.findPathDescriptor()
        val relativePath = path.getRelativePathFor(pathDescriptor)

        val fileDescriptor = path_open(
            fd = pathDescriptor,
            dirflags = 0,
            path = relativePath,
            oflags = OFLAGS.CREAT,
            fs_rights_base = RIGHTS.FD_WRITE or RIGHTS.PATH_CREATE_FILE or RIGHTS.PATH_CREATE_DIRECTORY or RIGHTS.PATH_OPEN,
            fs_rights_inheriting = 0,
            fdflags = 0,
        )

        wasiFileWrite(
            fd = fileDescriptor,
            ivos = listOf(content.encodeToByteArray())
        )

        fd_close(fileDescriptor)
    }

    override fun arguments(): Array<out String> =
        wasiGetArguments().toTypedArray()

    override fun getMeasurer(): Measurer = WasiMeasurer()

    override fun isSupported(): Boolean = true
}

private class WasiMeasurer : Measurer() {
    private var start: Long = 0L
    override fun measureStart() {
        start = getTimeNanoseconds()
    }

    override fun measureFinish(): Long {
        val end = getTimeNanoseconds()
        return end - start
    }
}

internal actual var engineSupport: BenchmarkEngineSupport = WasiEngineSupport