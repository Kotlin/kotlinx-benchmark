@file:OptIn(UnsafeWasmMemoryApi::class)

package kotlinx.benchmark.wasm.wasi

import kotlin.Short
import kotlin.wasm.WasmImport
import kotlin.wasm.unsafe.*

internal typealias Size = Int

internal typealias Filesize = Long

internal typealias Timestamp = Long

internal enum class Clockid {
    /**
     * The store-wide monotonic clock, which is defined as a clock measuring real time, whose value
     * cannot be adjusted and which cannot have negative clock jumps. The epoch of this clock is
     * undefined. The absolute time value of this clock therefore has no meaning.
     */
    MONOTONIC,
}

internal enum class Errno {
    /** No error occurred. System call completed successfully. */
    SUCCESS,
    /** Argument list too long. */
    _2BIG,
    /** Permission denied. */
    ACCES,
    /** Address in use. */
    ADDRINUSE,
    /** Address not available. */
    ADDRNOTAVAIL,
    /** Address family not supported. */
    AFNOSUPPORT,
    /** Resource unavailable, or operation would block. */
    AGAIN,
    /** Connection already in progress. */
    ALREADY,
    /** Bad file descriptor. */
    BADF,
    /** Bad message. */
    BADMSG,
    /** Device or resource busy. */
    BUSY,
    /** Operation canceled. */
    CANCELED,
    /** No child processes. */
    CHILD,
    /** Connection aborted. */
    CONNABORTED,
    /** Connection refused. */
    CONNREFUSED,
    /** Connection reset. */
    CONNRESET,
    /** Resource deadlock would occur. */
    DEADLK,
    /** Destination address required. */
    DESTADDRREQ,
    /** Mathematics argument out of domain of function. */
    DOM,
    /** Reserved. */
    DQUOT,
    /** File exists. */
    EXIST,
    /** Bad address. */
    FAULT,
    /** File too large. */
    FBIG,
    /** Host is unreachable. */
    HOSTUNREACH,
    /** Identifier removed. */
    IDRM,
    /** Illegal byte sequence. */
    ILSEQ,
    /** Operation in progress. */
    INPROGRESS,
    /** Interrupted function. */
    INTR,
    /** Invalid argument. */
    INVAL,
    /** I/O error. */
    IO,
    /** Socket is connected. */
    ISCONN,
    /** Is a directory. */
    ISDIR,
    /** Too many levels of symbolic links. */
    LOOP,
    /** File descriptor value too large. */
    MFILE,
    /** Too many links. */
    MLINK,
    /** Message too large. */
    MSGSIZE,
    /** Reserved. */
    MULTIHOP,
    /** Filename too long. */
    NAMETOOLONG,
    /** Network is down. */
    NETDOWN,
    /** Connection aborted by network. */
    NETRESET,
    /** Network unreachable. */
    NETUNREACH,
    /** Too many files open in system. */
    NFILE,
    /** No buffer space available. */
    NOBUFS,
    /** No such device. */
    NODEV,
    /** No such file or directory. */
    NOENT,
    /** Executable file format error. */
    NOEXEC,
    /** No locks available. */
    NOLCK,
    /** Reserved. */
    NOLINK,
    /** Not enough space. */
    NOMEM,
    /** No message of the desired type. */
    NOMSG,
    /** Protocol not available. */
    NOPROTOOPT,
    /** No space left on device. */
    NOSPC,
    /** Function not supported. */
    NOSYS,
    /** The socket is not connected. */
    NOTCONN,
    /** Not a directory or a symbolic link to a directory. */
    NOTDIR,
    /** Directory not empty. */
    NOTEMPTY,
    /** State not recoverable. */
    NOTRECOVERABLE,
    /** Not a socket. */
    NOTSOCK,
    /** Not supported, or operation not supported on socket. */
    NOTSUP,
    /** Inappropriate I/O control operation. */
    NOTTY,
    /** No such device or address. */
    NXIO,
    /** Value too large to be stored in data type. */
    OVERFLOW,
    /** Previous owner died. */
    OWNERDEAD,
    /** Operation not permitted. */
    PERM,
    /** Broken pipe. */
    PIPE,
    /** Protocol error. */
    PROTO,
    /** Protocol not supported. */
    PROTONOSUPPORT,
    /** Protocol wrong type for socket. */
    PROTOTYPE,
    /** Result too large. */
    RANGE,
    /** Read-only file system. */
    ROFS,
    /** Invalid seek. */
    SPIPE,
    /** No such process. */
    SRCH,
    /** Reserved. */
    STALE,
    /** Connection timed out. */
    TIMEDOUT,
    /** Text file busy. */
    TXTBSY,
    /** Cross-device link. */
    XDEV,
    /** Extension: Capabilities insufficient. */
    NOTCAPABLE,
}

internal typealias Rights = Long

internal object RIGHTS {
    /**
     * The right to invoke `fd_read` and `sock_recv`. If `rights::fd_seek` is set, includes the right
     * to invoke `fd_pread`.
     */
    const val FD_READ: Rights = 1 shl 1
    /**
     * The right to invoke `fd_write` and `sock_send`. If `rights::fd_seek` is set, includes the right
     * to invoke `fd_pwrite`.
     */
    const val FD_WRITE: Rights = 1 shl 6
    /** The right to invoke `path_create_directory`. */
    const val PATH_CREATE_DIRECTORY: Rights = 1 shl 9
    /** If `path_open` is set, the right to invoke `path_open` with `oflags::creat`. */
    const val PATH_CREATE_FILE: Rights = 1 shl 10
    /** The right to invoke `path_open`. */
    const val PATH_OPEN: Rights = 1 shl 13
    /** The right to invoke `fd_filestat_get`. */
    const val FD_FILESTAT_GET: Rights = 1 shl 21
}

internal typealias Fd = Int

internal data class __unsafe__Iovec(
    /** The address of the buffer to be filled. */
    var buf: Pointer /*<Byte>*/,
    /** The length of the buffer to be filled. */
    var buf_len: Size,
)

internal fun __store___unsafe__Iovec(x: __unsafe__Iovec, ptr: Pointer) {
    (ptr + 0).storeInt(x.buf.address.toInt())
    (ptr + 4).storeInt(x.buf_len)
}

internal data class __unsafe__Ciovec(
    /** The address of the buffer to be written. */
    var buf: Pointer /*<Byte>*/,
    /** The length of the buffer to be written. */
    var buf_len: Size,
)

internal fun __store___unsafe__Ciovec(x: __unsafe__Ciovec, ptr: Pointer) {
    (ptr + 0).storeInt(x.buf.address.toInt())
    (ptr + 4).storeInt(x.buf_len)
}

internal typealias __unsafe__IovecArray = List<__unsafe__Iovec>

internal typealias __unsafe__CiovecArray = List<__unsafe__Ciovec>

internal typealias Inode = Long

internal enum class Filetype {
    /**
     * The type of the file descriptor or file is unknown or is different from any of the other types
     * specified.
     */
    UNKNOWN,
    /** The file descriptor or file refers to a block device inode. */
    BLOCK_DEVICE,
    /** The file descriptor or file refers to a character device inode. */
    CHARACTER_DEVICE,
    /** The file descriptor or file refers to a directory inode. */
    DIRECTORY,
    /** The file descriptor or file refers to a regular file inode. */
    REGULAR_FILE,
    /** The file descriptor or file refers to a datagram socket. */
    SOCKET_DGRAM,
    /** The file descriptor or file refers to a byte-stream socket. */
    SOCKET_STREAM,
    /** The file refers to a symbolic link inode. */
    SYMBOLIC_LINK,
}


internal typealias Fdflags = Short

internal typealias Device = Long

internal typealias Lookupflags = Int

internal typealias Oflags = Short

internal object OFLAGS {
    /** Create file if it does not exist. */
    const val CREAT: Oflags = (1 shl 0).toShort()
}

internal typealias Linkcount = Long

internal data class Filestat(
    /** Device ID of device containing the file. */
    var dev: Device,
    /** File serial number. */
    var ino: Inode,
    /** File type. */
    var filetype: Filetype,
    /** Number of hard links to the file. */
    var nlink: Linkcount,
    /**
     * For regular files, the file size in bytes. For symbolic links, the length in bytes of the
     * pathname contained in the symbolic link.
     */
    var size: Filesize,
    /** Last data access timestamp. */
    var atim: Timestamp,
    /** Last data modification timestamp. */
    var mtim: Timestamp,
    /** Last file status change timestamp. */
    var ctim: Timestamp,
)


internal data class PrestatDir(
    /** The length of the directory name for use with `fd_prestat_dir_name`. */
    var pr_name_len: Size,
)

internal sealed class Prestat {
    data class dir(var value: PrestatDir) : Prestat()
}

/**
 * Read command-line argument data. The size of the array should match that returned by
 * `args_sizes_get`. Each argument is expected to be `\0` terminated.
 */
internal fun __unsafe__args_get(
    argv: Pointer /*<Pointer/*<Byte>*/>*/,
    argv_buf: Pointer /*<Byte>*/,
) {
    val ret = _raw_wasm__args_get(argv.address.toInt(), argv_buf.address.toInt())
    if (ret != 0) {
        throw WasiError(Errno.entries[ret])
    }
}

/** Return command-line argument data sizes. */
///
/// ## Return
///
/// Returns the number of arguments and the size of the argument string
/// data, or an error.
internal fun args_sizes_get(): Pair<Size, Size> {
    withScopedMemoryAllocator { allocator ->
        val rp0 = allocator.allocate(4)
        val rp1 = allocator.allocate(4)
        val ret = _raw_wasm__args_sizes_get(rp0.address.toInt(), rp1.address.toInt())
        return if (ret == 0) {
            Pair(
                Pointer(rp0.address).loadInt(),
                (Pointer(rp1.address)).loadInt())
        } else {
            throw WasiError(Errno.entries[ret])
        }
    }
}


/** Close a file descriptor. Note: This is similar to `close` in POSIX. */
internal fun fd_close(
    fd: Fd,
) {
    val ret = _raw_wasm__fd_close(fd)
    if (ret != 0) {
        throw WasiError(Errno.entries[ret])
    }
}

/** Return the attributes of an open file. */
///
/// ## Return
///
/// The buffer where the file's attributes are stored.
internal fun fd_filestat_get(
    fd: Fd,
): Filestat {
    withScopedMemoryAllocator { allocator ->
        val rp0 = allocator.allocate(64)
        val ret = _raw_wasm__fd_filestat_get(fd, rp0.address.toInt())
        return if (ret == 0) {
            Filestat(
                (Pointer(rp0.address) + 0).loadLong(),
                (Pointer(rp0.address) + 8).loadLong(),
                Filetype.entries[(Pointer(rp0.address) + 16).loadByte().toInt()],
                (Pointer(rp0.address) + 24).loadLong(),
                (Pointer(rp0.address) + 32).loadLong(),
                (Pointer(rp0.address) + 40).loadLong(),
                (Pointer(rp0.address) + 48).loadLong(),
                (Pointer(rp0.address) + 56).loadLong(),
            )
        } else {
            throw WasiError(Errno.values()[ret])
        }
    }
}

/** Return a description of the given preopened file descriptor. */
///
/// ## Return
///
/// The buffer where the description is stored.
internal fun fd_prestat_get(
    fd: Fd,
): Prestat {
    withScopedMemoryAllocator { allocator ->
        val rp0 = allocator.allocate(8)
        val ret = _raw_wasm__fd_prestat_get(fd, rp0.address.toInt())
        return if (ret == 0) {
            when (Pointer(rp0.address).loadByte().toInt()) {
                0 -> {
                    Prestat.dir(
                        PrestatDir(
                            (Pointer(rp0.address) + 4 + 0).loadInt(),
                        ))
                }
                else -> error("Invalid variant")
            }
        } else {
            throw WasiError(Errno.entries[ret])
        }
    }
}

internal fun __unsafe__fd_prestat_dir_name(
    fd: Fd,
    path: Pointer /*<Byte>*/,
    path_len: Size,
) {
    val ret = _raw_wasm__fd_prestat_dir_name(fd, path.address.toInt(), path_len)
    if (ret != 0) {
        throw WasiError(Errno.entries[ret])
    }
}


/** Read from a file descriptor. Note: This is similar to `readv` in POSIX. */
///
/// ## Parameters
///
/// * `iovs` - List of scatter/gather vectors to which to store data.
///
/// ## Return
///
/// The number of bytes read.
internal fun __unsafe__fd_read(
    allocator: MemoryAllocator,
    fd: Fd,
    iovs: __unsafe__IovecArray,
): Size {
    val rp0 = allocator.allocate(4)
    val ret =
        _raw_wasm__fd_read(
            fd,
            allocator.writeToLinearMemory(iovs).address.toInt(),
            iovs.size,
            rp0.address.toInt())
    return if (ret == 0) {
        Pointer(rp0.address).loadInt()
    } else {
        throw WasiError(Errno.entries[ret])
    }
}


/** Write to a file descriptor. Note: This is similar to `writev` in POSIX. */
///
/// ## Parameters
///
/// * `iovs` - List of scatter/gather vectors from which to retrieve data.
internal fun __unsafe__fd_write(
    allocator: MemoryAllocator,
    fd: Fd,
    iovs: __unsafe__CiovecArray,
): Size {
    val rp0 = allocator.allocate(4)
    val ret =
        _raw_wasm__fd_write(
            fd,
            allocator.writeToLinearMemory(iovs).address.toInt(),
            iovs.size,
            rp0.address.toInt())
    return if (ret == 0) {
        Pointer(rp0.address).loadInt()
    } else {
        throw WasiError(Errno.entries[ret])
    }
}

internal fun path_open(
    fd: Fd,
    dirflags: Lookupflags,
    path: String,
    oflags: Oflags,
    fs_rights_base: Rights,
    fs_rights_inheriting: Rights,
    fdflags: Fdflags,
): Fd {
    withScopedMemoryAllocator { allocator ->
        val rp0 = allocator.allocate(4)
        val ret =
            _raw_wasm__path_open(
                fd,
                dirflags,
                allocator.writeToLinearMemory(path).address.toInt(),
                path.size,
                oflags.toInt(),
                fs_rights_base,
                fs_rights_inheriting,
                fdflags.toInt(),
                rp0.address.toInt())
        return if (ret == 0) {
            Pointer(rp0.address).loadInt()
        } else {
            throw WasiError(Errno.entries[ret])
        }
    }
}

internal fun getTimeNanoseconds(): Long = withScopedMemoryAllocator { allocator ->
    val rp0 = allocator.allocate(8)
    val clockId = Clockid.MONOTONIC.ordinal
    val precision = 1L
    val rp0Address = rp0.address.toInt()
    _raw_wasm__clock_time_get(clockId, precision, rp0Address)
    return Pointer(rp0Address.toUInt()).loadLong()
}


/**
 * Read command-line argument data. The size of the array should match that returned by
 * `args_sizes_get`. Each argument is expected to be `\0` terminated.
 */
@WasmImport("wasi_snapshot_preview1", "args_get")
private external fun _raw_wasm__args_get(
    arg0: Int,
    arg1: Int,
): Int
/** Return command-line argument data sizes. */
@WasmImport("wasi_snapshot_preview1", "args_sizes_get")
private external fun _raw_wasm__args_sizes_get(
    arg0: Int,
    arg1: Int,
): Int

/** Return the time value of a clock. Note: This is similar to `clock_gettime` in POSIX. */
@WasmImport("wasi_snapshot_preview1", "clock_time_get")
internal external fun _raw_wasm__clock_time_get(
    arg0: Int,
    arg1: Long,
    arg2: Int,
): Int

/** Close a file descriptor. Note: This is similar to `close` in POSIX. */
@WasmImport("wasi_snapshot_preview1", "fd_close")
private external fun _raw_wasm__fd_close(
    arg0: Int,
): Int

/** Return the attributes of an open file. */
@WasmImport("wasi_snapshot_preview1", "fd_filestat_get")
private external fun _raw_wasm__fd_filestat_get(
    arg0: Int,
    arg1: Int,
): Int

/** Return a description of the given preopened file descriptor. */
@WasmImport("wasi_snapshot_preview1", "fd_prestat_get")
private external fun _raw_wasm__fd_prestat_get(
    arg0: Int,
    arg1: Int,
): Int
/** Return a description of the given preopened file descriptor. */
@WasmImport("wasi_snapshot_preview1", "fd_prestat_dir_name")
private external fun _raw_wasm__fd_prestat_dir_name(
    arg0: Int,
    arg1: Int,
    arg2: Int,
): Int


/** Read from a file descriptor. Note: This is similar to `readv` in POSIX. */
@WasmImport("wasi_snapshot_preview1", "fd_read")
private external fun _raw_wasm__fd_read(
    arg0: Int,
    arg1: Int,
    arg2: Int,
    arg3: Int,
): Int

/** Write to a file descriptor. Note: This is similar to `writev` in POSIX. */
@WasmImport("wasi_snapshot_preview1", "fd_write")
private external fun _raw_wasm__fd_write(
    arg0: Int,
    arg1: Int,
    arg2: Int,
    arg3: Int,
): Int

/**
 * Open a file or directory. The returned file descriptor is not guaranteed to be the
 * lowest-numbered file descriptor not currently open; it is randomized to prevent applications from
 * depending on making assumptions about indexes, since this is error-prone in multi-threaded
 * contexts. The returned file descriptor is guaranteed to be less than 2**31. Note: This is similar
 * to `openat` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "path_open")
private external fun _raw_wasm__path_open(
    arg0: Int,
    arg1: Int,
    arg2: Int,
    arg3: Int,
    arg4: Int,
    arg5: Long,
    arg6: Long,
    arg7: Int,
    arg8: Int,
): Int
