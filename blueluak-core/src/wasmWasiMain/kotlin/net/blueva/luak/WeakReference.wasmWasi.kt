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

// Same trade-off as WeakReference.nonJvm.kt (JS/Wasm-JS): Kotlin/Wasm has no
// portable weak-reference primitive available here either, so this holds a
// strong reference. get() never returns null.
actual class WeakReference<T : Any> actual constructor(private val referent: T) {
    actual fun get(): T? = referent
}
