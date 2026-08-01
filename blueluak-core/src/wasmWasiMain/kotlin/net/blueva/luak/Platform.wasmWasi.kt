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

import kotlin.reflect.KClass
import kotlin.wasm.ExperimentalWasmInterop
import kotlin.wasm.WasmImport

// Identical to Platform.nonJvm.kt (JS/Wasm-JS): none of this needs a JS host,
// so it's just as portable under WASI. Not shared via a common source set
// with jsHostMain/nonJvmMain to keep wasmWasiMain fully independent of any
// JS-hosted assumptions; this file is intentionally small to duplicate.
internal actual fun currentTimeMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
internal actual fun platformProperty(name: String): String? = null

@OptIn(ExperimentalWasmInterop::class)
@WasmImport("wasi_snapshot_preview1", "proc_exit")
private external fun wasiProcExit(code: Int): Unit

internal actual fun platformExit(code: Int) {
    wasiProcExit(code)
}

internal actual fun platformCollectGarbage() = Unit
internal actual fun platformUsedMemory(): Long = 0L
internal actual fun platformLoadLibrary(className: String, globals: Globals): LuaValue? = null
internal actual fun platformTypeName(type: KClass<*>): String = type.simpleName ?: "userdata"
