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

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.Worker

@OptIn(ExperimentalAtomicApi::class, ObsoleteWorkersApi::class)
internal actual class CoroutineRunner actual constructor(private val action: () -> Unit) {
    private val state = AtomicInt(STATE_INITIAL)
    private var worker: Worker? = null

    actual fun startAndWait(name: String) {
        check(state.compareAndSet(STATE_INITIAL, STATE_RUNNING)) { "Coroutine already started" }
        worker = Worker.start(name = name).also { coroutineWorker ->
            coroutineWorker.executeAfter { action() }
        }
        waitUntilPaused()
    }

    actual fun resumeAndWait() {
        check(state.compareAndSet(STATE_SUSPENDED, STATE_RUNNING)) { "Coroutine is not suspended" }
        waitUntilPaused()
    }

    actual fun yieldAndWait(checkIntervalMillis: Long, isAlive: () -> Boolean) {
        check(state.compareAndSet(STATE_RUNNING, STATE_SUSPENDED)) { "Coroutine is not running" }
        val checkIntervalMicros = checkIntervalMillis.coerceAtLeast(1) * MICROS_PER_MILLI
        while (state.load() == STATE_SUSPENDED) {
            Worker.current.park(checkIntervalMicros, process = false)
            isAlive()
        }
    }

    actual fun complete() {
        state.store(STATE_COMPLETED)
    }

    private fun waitUntilPaused() {
        while (state.load() == STATE_RUNNING) {
            Worker.current.park(POLL_INTERVAL_MICROS, process = false)
        }
        if (state.load() == STATE_COMPLETED) {
            worker?.requestTermination(processScheduledJobs = false)?.result
            worker = null
        }
    }

    private companion object {
        const val STATE_INITIAL = 0
        const val STATE_RUNNING = 1
        const val STATE_SUSPENDED = 2
        const val STATE_COMPLETED = 3
        const val MICROS_PER_MILLI = 1_000L
        const val POLL_INTERVAL_MICROS = 1_000L
    }
}
