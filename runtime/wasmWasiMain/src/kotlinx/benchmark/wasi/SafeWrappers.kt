@file:OptIn(UnsafeWasmMemoryApi::class)

package kotlinx.benchmark.wasm.wasi

import kotlin.wasm.unsafe.*

internal fun wasiGetArguments(): List<String> = withScopedMemoryAllocator { allocator ->
    val (numArgs, bufSize) = args_sizes_get()
    val byteArrayBuf = ByteArray(bufSize)

    val argv = allocator.allocate(numArgs * PTR_SIZE)
    val argv_buf = allocator.allocate(bufSize)
    __unsafe__args_get(argv, argv_buf)
    val result = mutableListOf<String>()
    repeat(numArgs) { idx ->
        val argv_ptr = argv + idx * PTR_SIZE
        val ptr = Pointer(argv_ptr.loadInt().toUInt())
        val endIndex = readZeroTerminatedByteArray(ptr, byteArrayBuf)
        val str = byteArrayBuf.decodeToString(endIndex = endIndex)
        result += str
    }
    result
}

internal fun wasiFileRead(fd: Fd, iovs: List<ByteArray>): Size = withScopedMemoryAllocator { allocator ->
    val iovs_mem = iovs.map { __unsafe__Iovec(allocator.writeToLinearMemory(it), it.size) }
    val result = __unsafe__fd_read(allocator, fd, iovs_mem)
    iovs.forEachIndexed { index, iovec ->
        val inArray = iovs_mem[index]
        repeat(iovec.size) { idx ->
            iovec[idx] = (inArray.buf + idx).loadByte()
        }
    }
    result
}

internal fun wasiFileWrite(fd: Fd, ivos: List<ByteArray>): Size = withScopedMemoryAllocator { allocator ->
    val iovs = ivos.map { __unsafe__Ciovec(allocator.writeToLinearMemory(it), it.size) }
    __unsafe__fd_write(allocator, fd, iovs)
}

internal fun wasiPrestatDirectoryName(fd: Fd): String = withScopedMemoryAllocator { allocator ->
    val pathLenght = (fd_prestat_get(fd) as Prestat.dir).value.pr_name_len
    val path = allocator.allocate(pathLenght)
    __unsafe__fd_prestat_dir_name(fd, path, pathLenght)
    loadString(path, pathLenght)
}
