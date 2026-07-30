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


import java.lang.ref.WeakReference

/**
 * Subclass of [LuaValue] that implements
 * a lua coroutine thread using Java Threads.
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
 * The utility classes [net.blueva.luak.lib.jse.JsePlatform] and
 * [net.blueva.luak.lib.jme.JmePlatform]
 * see to it that this [Globals] are initialized properly.
 * 
 * 
 * The behavior of coroutine threads matches closely the behavior
 * of C coroutine library.  However, because of the use of Java threads
 * to manage call state, it is possible to yield from anywhere in luaj.
 * 
 * 
 * Each Java thread wakes up at regular intervals and checks a weak reference
 * to determine if it can ever be resumed.  If not, it throws
 * [OrphanedThread] which is an [java.lang.Error].
 * Applications should not catch [OrphanedThread], because it can break
 * the thread safety of luaj.  The value controlling the polling interval
 * is [.thread_orphan_check_interval] and may be set by the user.
 * 
 * 
 * There are two main ways to abandon a coroutine.  The first is to call
 * `yield()` from lua, or equivalently [Globals.yield],
 * and arrange to have it never resumed possibly by values passed to yield.
 * The second is to throw [OrphanedThread], which should put the thread
 * in a dead state.   In either case all references to the thread must be
 * dropped, and the garbage collector must run for the thread to be
 * garbage collected.
 * 
 * 
 * @see LuaValue
 * 
 * @see net.blueva.luak.lib.jse.JsePlatform
 * 
 * @see net.blueva.luak.lib.jme.JmePlatform
 * 
 * @see net.blueva.luak.lib.CoroutineLib
 */
class LuaThread : LuaValue {
    val state: State

    /** Thread-local used by DebugLib to store debugging state.
     * This is an opaque value that should not be modified by applications.  */
    var callstack: Object? = null

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

    class State internal constructor(globals: Globals, lua_thread: LuaThread?, function: LuaValue?) : Runnable {
        private val globals: Globals
        val lua_thread: WeakReference
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

        init {
            this.globals = globals
            this.lua_thread = WeakReference(lua_thread)
            this.function = function
        }

        @kotlin.jvm.Synchronized
        override fun run() {
            try {
                val a: Varargs? = this.args
                this.args = LuaValue.NONE
                this.result = function.invoke(a)
            } catch (t: Throwable) {
                this.error = t.message
            } finally {
                this.status = net.blueva.luak.LuaThread.Companion.STATUS_DEAD
                this.notify()
            }
        }

        @kotlin.jvm.Synchronized
        fun lua_resume(new_thread: LuaThread?, args: Varargs?): Varargs {
            val previous_thread: LuaThread? = globals.running
            try {
                globals.running = new_thread
                this.args = args
                if (this.status == net.blueva.luak.LuaThread.Companion.STATUS_INITIAL) {
                    this.status = net.blueva.luak.LuaThread.Companion.STATUS_RUNNING
                    Thread(this, "Coroutine-" + (++net.blueva.luak.LuaThread.Companion.coroutine_count)).start()
                } else {
                    this.notify()
                }
                if (previous_thread != null) previous_thread.state.status =
                    net.blueva.luak.LuaThread.Companion.STATUS_NORMAL
                this.status = net.blueva.luak.LuaThread.Companion.STATUS_RUNNING
                this.wait()
                return (if (this.error != null) LuaValue.varargsOf(
                    LuaValue.FALSE,
                    LuaValue.valueOf(this.error)
                ) else LuaValue.varargsOf(LuaValue.TRUE, this.result))
            } catch (ie: InterruptedException) {
                throw OrphanedThread()
            } finally {
                this.args = LuaValue.NONE
                this.result = LuaValue.NONE
                this.error = null
                globals.running = previous_thread
                if (previous_thread != null) globals.running.state.status =
                    net.blueva.luak.LuaThread.Companion.STATUS_RUNNING
            }
        }

        @kotlin.jvm.Synchronized
        fun lua_yield(args: Varargs?): Varargs? {
            try {
                this.result = args
                this.status = net.blueva.luak.LuaThread.Companion.STATUS_SUSPENDED
                this.notify()
                do {
                    this.wait(net.blueva.luak.LuaThread.Companion.thread_orphan_check_interval)
                    if (this.lua_thread.get() == null) {
                        this.status = net.blueva.luak.LuaThread.Companion.STATUS_DEAD
                        throw OrphanedThread()
                    }
                } while (this.status == net.blueva.luak.LuaThread.Companion.STATUS_SUSPENDED)
                return this.args
            } catch (ie: InterruptedException) {
                this.status = net.blueva.luak.LuaThread.Companion.STATUS_DEAD
                throw OrphanedThread()
            } finally {
                this.args = LuaValue.NONE
                this.result = LuaValue.NONE
            }
        }
    }

    companion object {
        /** Shared metatable for lua threads.  */
        var s_metatable: LuaValue? = null

        /** The current number of coroutines.  Should not be set.  */
        var coroutine_count: Int = 0

        /** Polling interval, in milliseconds, which each thread uses while waiting to
         * return from a yielded state to check if the lua threads is no longer
         * referenced and therefore should be garbage collected.
         * A short polling interval for many threads will consume server resources.
         * Orphaned threads cannot be detected and collected unless garbage
         * collection is run.  This can be changed by Java startup code if desired.
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
