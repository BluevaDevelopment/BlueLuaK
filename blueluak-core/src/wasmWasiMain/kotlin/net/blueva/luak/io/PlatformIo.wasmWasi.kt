/******************************************************************************
 *  ____  _            _                _  __
 * | __ )| |_   _  ___| |   _   _  __ _| |/ /
 * |  _ \| | | | |/ _ \ |  | | | |/ _` | ' /
 * | |_) | | |_| |  __/ |__| |_| | (_| | . \
 * |____/|_|\__,_|\___|_____\__,_|\__,_|_|\_\
 *
 *  BlueLuaK
 *  https://github.com/BluevaDevelopment/BlueLuaK
 *
 *  Copyright (c) 2026 Blueva Development
 *
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package net.blueva.luak.io

import kotlin.wasm.ExperimentalWasmInterop
import kotlin.wasm.WasmImport
import kotlin.wasm.unsafe.MemoryAllocator
import kotlin.wasm.unsafe.Pointer
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator

/**
 * standardOutput()/standardError()/platformResource() for a raw WASI
 * (`wasi_snapshot_preview1`) host: no JS engine, no `node:fs`, no `process` -
 * just the WASI syscalls every conforming host (wasmtime, wasmer, Node's
 * `node:wasi`, ...) implements. This mirrors, at the same low level, what the
 * Kotlin/Wasm-WASI standard library itself uses internally for `println()`
 * (`fd_write` via `@WasmImport`), so it needs no extra host configuration
 * beyond what any Kotlin/Wasm-WASI program already requires.
 *
 * Capability model: WASI grants filesystem access only through pre-opened
 * directory file descriptors the *host* chooses to hand the module (e.g.
 * `wasmtime run --dir=. module.wasm`, or an equivalent `preopens` entry for
 * an embedding host). [platformResource] walks those pre-opens (starting at
 * fd 3; fds 0-2 are stdio) and tries `path_open` against each, so it works
 * regardless of what name the host gave its preopened directory. If the host
 * granted no filesystem capability at all, every lookup deterministically
 * returns null (matching the `expect fun platformResource(...): InputStream?`
 * contract) rather than hanging or throwing a host-specific error.
 */

private const val WASI_ERRNO_SUCCESS = 0
private const val WASI_PREOPENTYPE_DIR = 0
private const val WASI_RIGHTS_FD_READ = 1L shl 1
private const val WASI_STDOUT_FD = 1
private const val WASI_STDERR_FD = 2
private const val FIRST_PREOPEN_FD = 3
private const val MAX_PREOPEN_FD_TO_TRY = 64
private const val READ_CHUNK_SIZE = 4096

@OptIn(ExperimentalWasmInterop::class)
@WasmImport("wasi_snapshot_preview1", "fd_write")
private external fun wasiFdWrite(fd: Int, iovsPtr: Int, iovsLen: Int, resultPtr: Int): Int

@OptIn(ExperimentalWasmInterop::class)
@WasmImport("wasi_snapshot_preview1", "fd_read")
private external fun wasiFdRead(fd: Int, iovsPtr: Int, iovsLen: Int, resultPtr: Int): Int

@OptIn(ExperimentalWasmInterop::class)
@WasmImport("wasi_snapshot_preview1", "fd_close")
private external fun wasiFdClose(fd: Int): Int

@OptIn(ExperimentalWasmInterop::class)
@WasmImport("wasi_snapshot_preview1", "fd_prestat_get")
private external fun wasiFdPrestatGet(fd: Int, resultPtr: Int): Int

@OptIn(ExperimentalWasmInterop::class)
@WasmImport("wasi_snapshot_preview1", "path_open")
private external fun wasiPathOpen(
    dirFd: Int,
    dirFlags: Int,
    pathPtr: Int,
    pathLen: Int,
    oFlags: Int,
    fsRightsBase: Long,
    fsRightsInheriting: Long,
    fdFlags: Int,
    resultPtr: Int,
): Int

@OptIn(UnsafeWasmMemoryApi::class)
private fun Pointer.storeBytes(bytes: ByteArray) {
    var p = this
    for (b in bytes) {
        p.storeByte(b)
        p += 1
    }
}

@OptIn(UnsafeWasmMemoryApi::class)
private fun wasiWrite(fd: Int, bytes: ByteArray, offset: Int, length: Int) {
    if (length == 0) return
    withScopedMemoryAllocator { allocator ->
        val dataPtr = allocator.allocate(length)
        var p = dataPtr
        for (i in offset until offset + length) {
            p.storeByte(bytes[i])
            p += 1
        }
        val iovPtr = allocator.allocate(8)
        iovPtr.storeInt(dataPtr.address.toInt())
        (iovPtr + 4).storeInt(length)
        val resultPtr = allocator.allocate(4)
        val errno = wasiFdWrite(fd, iovPtr.address.toInt(), 1, resultPtr.address.toInt())
        if (errno != WASI_ERRNO_SUCCESS) throw IOException("WASI fd_write failed with errno $errno")
    }
}

private class WasiOutputStream(private val fd: Int) : OutputStream() {
    override fun write(byte: Int) = wasiWrite(fd, byteArrayOf(byte.toByte()), 0, 1)
    override fun write(bytes: ByteArray, offset: Int, length: Int) = wasiWrite(fd, bytes, offset, length)
}

private val wasiStandardOutput = PrintStream(WasiOutputStream(WASI_STDOUT_FD))
private val wasiStandardError = PrintStream(WasiOutputStream(WASI_STDERR_FD))

actual fun standardOutput(): PrintStream = wasiStandardOutput
actual fun standardError(): PrintStream = wasiStandardError

/** Opens [path] relative to preopened directory [dirFd]; null (not thrown) on any failure. */
@OptIn(UnsafeWasmMemoryApi::class)
private fun tryPathOpen(allocator: MemoryAllocator, dirFd: Int, path: String): Int? {
    val pathBytes = path.encodeToByteArray()
    val pathPtr = allocator.allocate(pathBytes.size.coerceAtLeast(1))
    pathPtr.storeBytes(pathBytes)
    val resultPtr = allocator.allocate(4)
    val errno = wasiPathOpen(
        dirFd = dirFd,
        dirFlags = 0,
        pathPtr = pathPtr.address.toInt(),
        pathLen = pathBytes.size,
        oFlags = 0,
        fsRightsBase = WASI_RIGHTS_FD_READ,
        fsRightsInheriting = 0L,
        fdFlags = 0,
        resultPtr = resultPtr.address.toInt(),
    )
    if (errno != WASI_ERRNO_SUCCESS) return null
    return resultPtr.loadInt()
}

/** Tries [path] against every preopened directory the host granted us, in fd order. */
@OptIn(UnsafeWasmMemoryApi::class)
private fun openUnderAnyPreopen(allocator: MemoryAllocator, path: String): Int? {
    var fd = FIRST_PREOPEN_FD
    while (fd < MAX_PREOPEN_FD_TO_TRY) {
        // __wasi_prestat_t: { tag: u8, [3 bytes padding], dir.pr_name_len: u32 }, 8 bytes.
        val prestatPtr = allocator.allocate(8)
        val prestatErrno = wasiFdPrestatGet(fd, prestatPtr.address.toInt())
        if (prestatErrno != WASI_ERRNO_SUCCESS) break // no more preopened fds past this point
        val tag = prestatPtr.loadByte().toInt()
        if (tag == WASI_PREOPENTYPE_DIR) {
            tryPathOpen(allocator, fd, path)?.let { return it }
        }
        fd++
    }
    return null
}

@OptIn(UnsafeWasmMemoryApi::class)
private fun readAllAndClose(allocator: MemoryAllocator, fd: Int): ByteArray {
    try {
        val out = ByteArrayOutputStream()
        val bufPtr = allocator.allocate(READ_CHUNK_SIZE)
        val iovPtr = allocator.allocate(8)
        iovPtr.storeInt(bufPtr.address.toInt())
        (iovPtr + 4).storeInt(READ_CHUNK_SIZE)
        val resultPtr = allocator.allocate(4)
        while (true) {
            val errno = wasiFdRead(fd, iovPtr.address.toInt(), 1, resultPtr.address.toInt())
            if (errno != WASI_ERRNO_SUCCESS) break
            val n = resultPtr.loadInt()
            if (n <= 0) break
            var p = bufPtr
            repeat(n) {
                out.write(p.loadByte().toInt() and 0xff)
                p += 1
            }
        }
        return out.toByteArray()
    } finally {
        wasiFdClose(fd)
    }
}

@OptIn(UnsafeWasmMemoryApi::class)
actual fun platformResource(name: String): InputStream? {
    return withScopedMemoryAllocator { allocator ->
        val fd = openUnderAnyPreopen(allocator, name) ?: return@withScopedMemoryAllocator null
        ByteArrayInputStream(readAllAndClose(allocator, fd))
    }
}
