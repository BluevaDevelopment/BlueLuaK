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

// "file.separator" is OS-specific; everything else is a real env var lookup.
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

// Usage as of the last collection (0 until one has run); doesn't force a
// collection itself, unlike platformCollectGarbage.
@OptIn(NativeRuntimeApi::class, ExperimentalStdlibApi::class)
internal actual fun platformUsedMemory(): Long =
    GC.lastGCInfo?.memoryUsageAfter?.values?.sumOf { it.totalObjectsSizeBytes } ?: 0L

internal actual fun platformLoadLibrary(className: String, globals: Globals): LuaValue? = null
internal actual fun platformTypeName(type: KClass<*>): String = type.simpleName ?: "userdata"
