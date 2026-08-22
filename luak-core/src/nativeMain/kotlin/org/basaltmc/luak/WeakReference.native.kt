/******************************************************************************
 *  _                _
 * | |   _   _  __ _| | __
 * | |  | | | |/ _` | |/ /
 * | |__| |_| | (_| |   <
 * |_____\__,_|\__,_|_|\_\
 *
 *  Luak
 *  https://github.com/BasaltProject/Luak
 *
 *  Copyright (c) 2026 Basalt Project
 *
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package org.basaltmc.luak

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual class WeakReference<T : Any> actual constructor(referent: T) {
    private val delegate = kotlin.native.ref.WeakReference(referent)
    actual fun get(): T? = delegate.get()
}
