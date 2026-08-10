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
package net.blueva.luak

import kotlin.native.Platform as NativePlatform
import kotlin.native.OsFamily
import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi
import kotlin.reflect.KClass
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.exit
import platform.posix.getenv

internal actual fun currentTimeMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()

// `file.separator` is the one property callers rely on getting a correct,
// OS-appropriate value regardless of environment (PackageLib's FILE_SEP);
// everything else maps to a real environment variable lookup, covering both
// os.getenv() and ad hoc flags like the "CALLS"/"TRACE" debug switches.
@OptIn(ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)
internal actual fun platformProperty(name: String): String? {
    if (name == "file.separator") {
        return if (NativePlatform.osFamily == OsFamily.WINDOWS) "\\" else "/"
    }
    return getenv(name)?.toKString()
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformExit(code: Int): Unit = exit(code)

@OptIn(NativeRuntimeApi::class)
internal actual fun platformCollectGarbage() {
    GC.collect()
}

// Reflects heap usage as of the last completed collection (0 until the first
// one runs). Kotlin/Native has no "live now" heap snapshot API that doesn't
// itself force a collection, and collectgarbage("count") forcing a full stop-
// the-world GC on every call - unlike real Lua's incrementally tracked count
// - would be a surprising performance cliff for callers polling it in a loop.
@OptIn(NativeRuntimeApi::class, ExperimentalStdlibApi::class)
internal actual fun platformUsedMemory(): Long =
    GC.lastGCInfo?.memoryUsageAfter?.values?.sumOf { it.totalObjectsSizeBytes } ?: 0L

internal actual fun platformLoadLibrary(className: String, globals: Globals): LuaValue? = null
internal actual fun platformTypeName(type: KClass<*>): String = type.simpleName ?: "userdata"
