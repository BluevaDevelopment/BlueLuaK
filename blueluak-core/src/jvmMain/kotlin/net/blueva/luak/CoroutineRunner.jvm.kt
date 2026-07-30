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

internal actual class CoroutineRunner actual constructor(private val action: () -> Unit) {
    private val lock = Object()

    actual fun startAndWait(name: String) = synchronized(lock) {
        Thread(action, name).start()
        lock.wait()
    }

    actual fun resumeAndWait() = synchronized(lock) {
        lock.notify()
        lock.wait()
    }

    actual fun yieldAndWait(checkIntervalMillis: Long, isAlive: () -> Boolean) = synchronized(lock) {
        lock.notify()
        do {
            lock.wait(checkIntervalMillis)
        } while (isAlive())
    }

    actual fun complete() = synchronized(lock) { lock.notify() }
}
