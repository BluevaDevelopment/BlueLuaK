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

import junit.framework.TestCase
import net.blueva.luak.lib.OneArgFunction
import net.blueva.luak.lib.jvm.JvmPlatform.standardGlobals
import java.lang.ref.WeakReference

class OrphanedThreadTest : TestCase() {
    var globals: Globals? = null
    var luathread: LuaThread? = null
    var luathr_ref: WeakReference<*>? = null
    var function: LuaValue? = null
    var func_ref: WeakReference<*>? = null

    @Throws(Exception::class)
    override fun setUp() {
        LuaThread.thread_orphan_check_interval = 5
        globals = standardGlobals()
    }

    override fun tearDown() {
        LuaThread.thread_orphan_check_interval = 30000
    }

    @Throws(Exception::class)
    fun testCollectOrphanedNormalThread() {
        function = NormalFunction(globals!!)
        doTest(LuaValue.TRUE, LuaValue.ZERO)
    }

    @Throws(Exception::class)
    fun testCollectOrphanedEarlyCompletionThread() {
        function = EarlyCompletionFunction(globals!!)
        doTest(LuaValue.TRUE, LuaValue.ZERO)
    }

    @Throws(Exception::class)
    fun testCollectOrphanedAbnormalThread() {
        function = AbnormalFunction(globals!!)
        doTest(LuaValue.FALSE, LuaValue.valueOf("abnormal condition"))
    }

    @Throws(Exception::class)
    fun testCollectOrphanedClosureThread() {
        val script =
            "print('in closure, arg is '..(...))\n" +
                    "arg = coroutine.yield(1)\n" +
                    "print('in closure.2, arg is '..arg)\n" +
                    "arg = coroutine.yield(0)\n" +
                    "print('leakage in closure.3, arg is '..arg)\n" +
                    "return 'done'\n"
        function = globals!!.load(script, "script")
        doTest(LuaValue.TRUE, LuaValue.ZERO)
    }

    @Throws(Exception::class)
    fun testCollectOrphanedPcallClosureThread() {
        val script =
            "f = function(x)\n" +
                    "  print('in pcall-closure, arg is '..(x))\n" +
                    "  arg = coroutine.yield(1)\n" +
                    "  print('in pcall-closure.2, arg is '..arg)\n" +
                    "  arg = coroutine.yield(0)\n" +
                    "  print('leakage in pcall-closure.3, arg is '..arg)\n" +
                    "  return 'done'\n" +
                    "end\n" +
                    "print( 'pcall-closre.result:', pcall( f, ... ) )\n"
        function = globals!!.load(script, "script")
        doTest(LuaValue.TRUE, LuaValue.ZERO)
    }

    @Throws(Exception::class)
    fun testCollectOrphanedLoadCloasureThread() {
        val script =
            "t = { \"print \", \"'hello, \", \"world'\", }\n" +
                    "i = 0\n" +
                    "arg = ...\n" +
                    "f = function()\n" +
                    "	i = i + 1\n" +
                    "   print('in load-closure, arg is', arg, 'next is', t[i])\n" +
                    "   arg = coroutine.yield(1)\n" +
                    "	return t[i]\n" +
                    "end\n" +
                    "load(f)()\n"
        function = globals!!.load(script, "script")
        doTest(LuaValue.TRUE, LuaValue.ONE)
    }

    @Throws(Exception::class)
    private fun doTest(status2: LuaValue?, value2: LuaValue?) {
        luathread = LuaThread(globals!!, function)
        luathr_ref = WeakReference<Any?>(luathread)
        func_ref = WeakReference<Any?>(function)
        assertNotNull(luathr_ref!!.get())


        // resume two times
        var a = luathread!!.resume(LuaValue.valueOf("foo"))
        assertEquals(LuaValue.ONE, a.arg(2))
        assertEquals(LuaValue.TRUE, a.arg1())
        a = luathread!!.resume(LuaValue.valueOf("bar"))
        assertEquals(value2, a.arg(2))
        assertEquals(status2, a.arg1())


        // drop strong references
        luathread = null
        function = null


        // gc
        var i = 0
        while (i < 100 && (luathr_ref!!.get() != null || func_ref!!.get() != null)) {
            Runtime.getRuntime().gc()
            Thread.sleep(5)
            i++
        }


        // check reference
        assertNull(luathr_ref!!.get())
        assertNull(func_ref!!.get())
    }


    internal class NormalFunction(val globals: Globals) : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            var arg = arg
            println("in normal.1, arg is " + arg)
            arg = globals.yield(ONE).arg1()
            println("in normal.2, arg is " + arg)
            arg = globals.yield(ZERO).arg1()
            println("in normal.3, arg is " + arg)
            return NONE
        }
    }

    internal class EarlyCompletionFunction(val globals: Globals) : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            var arg = arg
            println("in early.1, arg is " + arg)
            arg = globals.yield(ONE).arg1()
            println("in early.2, arg is " + arg)
            return ZERO
        }
    }

    internal class AbnormalFunction(val globals: Globals) : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            var arg = arg
            println("in abnormal.1, arg is " + arg)
            arg = globals.yield(ONE).arg1()
            println("in abnormal.2, arg is " + arg)
            error("abnormal condition")
            return ZERO
        }
    }
}
