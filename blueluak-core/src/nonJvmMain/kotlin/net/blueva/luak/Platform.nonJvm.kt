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

internal actual fun currentTimeMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
internal actual fun platformProperty(name: String): String? = null
internal actual fun platformExit(code: Int) = Unit
internal actual fun platformCollectGarbage() = Unit
internal actual fun platformUsedMemory(): Long = 0L
internal actual fun platformLoadLibrary(className: String, globals: Globals): LuaValue? = null
