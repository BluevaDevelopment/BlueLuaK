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
 *  Based on LuaJ (https://luaj.org)
 *  Original work Copyright (c) 2009 Luaj.org
 *  Modifications Copyright (c) 2026 Blueva Development
 *
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package net.blueva.luak


import net.blueva.luak.WeakReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

/**
 * Subclass of [LuaValue] that implements a lua coroutine thread.
 *
 *
 * A LuaThread is typically created in response to a scripted call to
 * `coroutine.create()`
 *
 *
 * The threads must be initialized with the globals, so that
 * the global environment may be passed along according to rules of lua.
 * This is done via the constructor arguments [.LuaThread] or
 * [.LuaThread].
 *
 *
 * The utility classes [net.blueva.luak.lib.LuaPlatform] and
 * [net.blueva.luak.lib.jvm.JvmPlatform]
 * see to it that this [Globals] are initialized properly.
 *
 *
 * Resume/yield are implemented with Kotlin's own `suspend`/[kotlin.coroutines.Continuation]
 * machinery rather than a native thread or worker per coroutine, so this
 * works identically - and without blocking anything - on every KMP target,
 * including JS and Wasm where there is no thread to block. `yield()` only
 * suspends when called from Lua-to-Lua calls or from a function explicitly
 * written to propagate suspension (currently: `coroutine.yield` itself and
 * `pcall`/`xpcall`, matching real Lua 5.2's yieldable pcall). Calling it from
 * inside any other library function's own callback (e.g. `table.sort`'s
 * comparator) correctly raises "attempt to yield across metamethod/C-call
 * boundary", matching real Lua's C-call boundary restriction - unlike the
 * old Java-Threads-based implementation, which could yield from anywhere at
 * the cost of not being portable to JS/Wasm at all.
 *
 *
 * A suspended coroutine holds no more than a captured [kotlin.coroutines.Continuation]
 * and whatever it closed over; abandoning it (dropping all references without
 * ever resuming it again) is just ordinary garbage, collected normally,
 * with no orphan-thread bookkeeping required.
 *
 *
 * @see LuaValue
 *
 * @see net.blueva.luak.lib.jvm.JvmPlatform
 *
 * @see net.blueva.luak.lib.LuaPlatform
 *
 * @see net.blueva.luak.lib.CoroutineLib
 */
class LuaThread : LuaValue {
    val state: State

    /** Thread-local used by DebugLib to store debugging state.
     * This is an opaque value that should not be modified by applications.  */
    var callstack: Any? = null

    val globals: Globals?

    /** Error message handler for this thread, if any.   */
    var errorfunc: LuaValue? = null

    /** Private constructor for main thread only  */
    constructor(globals: Globals) {
        state = net.blueva.luak.LuaThread.State(globals, this, null)
        state.status = net.blueva.luak.LuaThread.Companion.STATUS_RUNNING
        this.globals = globals
    }

    /**
     * Create a LuaThread around a function and environment
     * @param func The function to execute
     */
    constructor(globals: Globals, func: LuaValue?) {
        LuaValue.assert_(func != null, "function cannot be null")
        state = net.blueva.luak.LuaThread.State(globals, this, func)
        this.globals = globals
    }

    override fun type(): Int {
        return LuaValue.TTHREAD
    }

    override fun typename(): String? {
        return "thread"
    }

    override fun isthread(): Boolean {
        return true
    }

    override fun optthread(defval: LuaThread?): LuaThread {
        return this
    }

    override fun checkthread(): LuaThread {
        return this
    }

    override fun getmetatable(): LuaValue? {
        return net.blueva.luak.LuaThread.Companion.s_metatable
    }

    val status: String?
        get() = net.blueva.luak.LuaThread.Companion.STATUS_NAMES[state.status]

    val isMainThread: Boolean
        get() = this.state.function == null

    fun resume(args: Varargs?): Varargs {
        val s = this.state
        if (s.status > net.blueva.luak.LuaThread.Companion.STATUS_SUSPENDED) return LuaValue.varargsOf(
            LuaValue.FALSE,
            LuaValue.valueOf("cannot resume " + (if (s.status == net.blueva.luak.LuaThread.Companion.STATUS_DEAD) "dead" else "non-suspended") + " coroutine")
        )
        return s.lua_resume(this, args)
    }

    /**
     * `coroutine.close`: ends this coroutine, running its pending closers.
     *
     * A suspended coroutine may be holding to-be-closed variables partway
     * through its body. Closing it unwinds from the point it yielded at, which
     * is what runs their `__close` handlers; an error raised by one of those is
     * reported rather than thrown.
     *
     * @return `true`, or `false` plus the error a closer raised
     */
    fun close(): Varargs {
        // Raised rather than reported: only a suspended or dead coroutine can
        // be closed, so anything else is a mistake in the call itself. The
        // status is looked at before the thread's identity, since the main
        // thread is "normal" while whatever it resumed is running.
        val s = this.state
        if (s.status == net.blueva.luak.LuaThread.Companion.STATUS_NORMAL) {
            LuaValue.error("cannot close a normal coroutine")
        }
        if (s.status == net.blueva.luak.LuaThread.Companion.STATUS_RUNNING) {
            if (this.isMainThread) LuaValue.error("cannot close main thread")
            LuaValue.error("cannot close a running coroutine")
        }
        return s.lua_close(this)
    }

    class State internal constructor(globals: Globals, lua_thread: LuaThread, function: LuaValue?) {
        private val globals: Globals
        val lua_thread: WeakReference<LuaThread>
        val function: LuaValue?
        var args: Varargs? = LuaValue.NONE
        var result: Varargs? = LuaValue.NONE
        var error: String? = null

        /** Hook function control state used by debug lib.  */
        var hookfunc: LuaValue? = null

        var hookline: Boolean = false
        var hookcall: Boolean = false
        var hookrtrn: Boolean = false
        var hookcount: Int = 0
        var inhook: Boolean = false
        var lastline: Int = 0
        var bytecodes: Int = 0

        var status: Int = net.blueva.luak.LuaThread.Companion.STATUS_INITIAL

        /** Continuation captured at this coroutine's most recent yield point, or
         * null if it has never yielded (not yet started, or already resumed
         * back to running). Resuming it continues Lua execution from exactly
         * where `coroutine.yield()` left off, with the resume() arguments
         * becoming yield()'s return values. */
        private var yieldContinuation: Continuation<Varargs>? = null

        /** Values passed to the most recent `coroutine.yield(...)` call, read by
         * lua_resume() once the resumed execution pauses there. */
        private var pendingYieldValues: Varargs? = null

        /** Set true exactly when the coroutine body has truly finished (returned
         * or thrown), as opposed to merely yielding. */
        private var finished = false
        private var finalResult: Result<Varargs>? = null

        private val completion = object : Continuation<Varargs> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Varargs>) {
                finished = true
                finalResult = result
                status = net.blueva.luak.LuaThread.Companion.STATUS_DEAD
            }
        }

        init {
            this.globals = globals
            this.lua_thread = WeakReference(lua_thread)
            this.function = function
        }

        fun lua_resume(new_thread: LuaThread, args: Varargs?): Varargs {
            val previous_thread: LuaThread = globals.running
            try {
                globals.running = new_thread
                // Mark the resuming thread NORMAL before running the resumed
                // thread, not after: the resumed thread may make its own
                // coroutine.status() calls on the resumer before yielding back.
                previous_thread.state.status = net.blueva.luak.LuaThread.Companion.STATUS_NORMAL
                status = net.blueva.luak.LuaThread.Companion.STATUS_RUNNING
                finished = false
                pendingYieldValues = null
                val contToResume = yieldContinuation
                yieldContinuation = null
                if (contToResume == null) {
                    val a: Varargs = args ?: LuaValue.NONE!!
                    val body: suspend () -> Varargs = { function!!.invokeSuspend(a) }
                    body.startCoroutine(completion)
                } else {
                    contToResume.resume(args ?: LuaValue.NONE!!)
                }
                return if (finished) {
                    val r = finalResult!!
                    val err = r.exceptionOrNull()
                    if (err != null) {
                        // A host error may carry no message of its own, and a
                        // resume still has to answer with something.
                        val text: String = err.message
                            ?: if (platformIsStackOverflow(err)) "stack overflow" else err.toString()
                        LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf(text))!!
                    }
                    else LuaValue.varargsOf(LuaValue.TRUE, r.getOrThrow())!!
                } else {
                    status = net.blueva.luak.LuaThread.Companion.STATUS_SUSPENDED
                    LuaValue.varargsOf(LuaValue.TRUE, pendingYieldValues ?: LuaValue.NONE!!)!!
                }
            } finally {
                finalResult = null
                pendingYieldValues = null
                globals.running = previous_thread
                globals.running.state.status = net.blueva.luak.LuaThread.Companion.STATUS_RUNNING
            }
        }

        /** Unwinds a suspended coroutine so its pending closers run. */
        fun lua_close(closing: LuaThread): Varargs {
            val continuation = yieldContinuation
            yieldContinuation = null
            if (continuation == null) {
                // Never started, or already finished: nothing is on its stack.
                status = net.blueva.luak.LuaThread.Companion.STATUS_DEAD
                return LuaValue.TRUE!!
            }
            val previous_thread: LuaThread = globals.running
            try {
                globals.running = closing
                previous_thread.state.status = net.blueva.luak.LuaThread.Companion.STATUS_NORMAL
                status = net.blueva.luak.LuaThread.Companion.STATUS_RUNNING
                finished = false
                finalResult = null
                // An Error rather than an Exception, so the interpreter's
                // catch-all leaves it alone and only the finally blocks - the
                // ones that close variables - run on the way out.
                continuation.resumeWithException(ClosedCoroutine())
            } finally {
                status = net.blueva.luak.LuaThread.Companion.STATUS_DEAD
                globals.running = previous_thread
                globals.running.state.status = net.blueva.luak.LuaThread.Companion.STATUS_RUNNING
            }
            val result = finalResult
            finalResult = null
            val failure: Throwable? = result?.exceptionOrNull()
            if (failure == null || failure is ClosedCoroutine) return LuaValue.TRUE!!
            return LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf(failure.message))!!
        }

        suspend fun lua_yield(args: Varargs?): Varargs {
            status = net.blueva.luak.LuaThread.Companion.STATUS_SUSPENDED
            pendingYieldValues = args ?: LuaValue.NONE
            if (this.lua_thread.get() == null) throw OrphanedThread()
            return suspendCoroutine { cont -> yieldContinuation = cont }
        }
    }

    /** Thrown into a suspended coroutine by [close] to unwind it. */
    internal class ClosedCoroutine : Error("coroutine closed")

    companion object {
        /** Shared metatable for lua threads.  */
        var s_metatable: LuaValue? = null

        /** The current number of coroutines.  Should not be set.  */
        var coroutine_count: Int = 0

        /** Unused: kept only for source/binary compatibility with code written
         * against the old Java-Threads-based coroutine implementation. Since
         * resume/yield are now backed by suspend/Continuation rather than a
         * blocked thread per coroutine, there is nothing left to poll - an
         * abandoned suspended coroutine is just unreachable memory, collected
         * the same way any other object is.
         */
        var thread_orphan_check_interval: Long = 5000

        const val STATUS_INITIAL: Int = 0
        const val STATUS_SUSPENDED: Int = 1
        const val STATUS_RUNNING: Int = 2
        const val STATUS_NORMAL: Int = 3
        const val STATUS_DEAD: Int = 4
        val STATUS_NAMES: Array<String?> = arrayOf<String?>(
            "suspended",
            "suspended",
            "running",
            "normal",
            "dead",
        )

        const val MAX_CALLSTACK: Int = 256
    }
}
