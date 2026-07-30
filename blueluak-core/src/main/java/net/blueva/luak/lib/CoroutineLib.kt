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
package net.blueva.luak.lib

import net.blueva.luak.Globals
import net.blueva.luak.LuaTable
import net.blueva.luak.LuaThread
import net.blueva.luak.LuaValue
import net.blueva.luak.Varargs

/**
 * Subclass of [LibFunction] which implements the lua standard `coroutine`
 * library.
 * 
 * 
 * The coroutine library in luaj has the same behavior as the
 * coroutine library in C, but is implemented using Java Threads to maintain
 * the call state between invocations.  Therefore it can be yielded from anywhere,
 * similar to the "Coco" yield-from-anywhere patch available for C-based lua.
 * However, coroutines that are yielded but never resumed to complete their execution
 * may not be collected by the garbage collector.
 * 
 * 
 * Typically, this library is included as part of a call to either
 * [net.blueva.luak.lib.jse.JsePlatform.standardGlobals] or [net.blueva.luak.lib.jme.JmePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals(); System.out.println( globals.get("coroutine").get("running").call() ); ` </pre>
 * 
 * 
 * To instantiate and use it directly,
 * link it into your globals table via [LuaValue.load] using code such as:
 * <pre> `Globals globals = new Globals(); globals.load(new JseBaseLib()); globals.load(new PackageLib()); globals.load(new CoroutineLib()); System.out.println( globals.get("coroutine").get("running").call() ); ` </pre>
 * 
 * 
 * @see LibFunction
 * 
 * @see net.blueva.luak.lib.jse.JsePlatform
 * 
 * @see net.blueva.luak.lib.jme.JmePlatform
 * 
 * @see [Lua 5.2 Coroutine Lib Reference](http://www.lua.org/manual/5.2/manual.html.6.2)
 */
class CoroutineLib : TwoArgFunction() {
    var globals: Globals? = null

    /** Perform one-time initialization on the library by creating a table
     * containing the library functions, adding that table to the supplied environment,
     * adding the table to package.loaded, and returning table as the return value.
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env the environment to load into, which must be a Globals instance.
     */
    override fun call(modname: LuaValue?, env: LuaValue?): LuaValue? {
        globals = env!!.checkglobals()
        val coroutine: LuaTable = LuaTable()
        coroutine.set("create", create())
        coroutine.set("resume", net.blueva.luak.lib.CoroutineLib.resume())
        coroutine.set("running", running())
        coroutine.set("status", net.blueva.luak.lib.CoroutineLib.status())
        coroutine.set("yield", YieldFunction())
        coroutine.set("wrap", wrap())
        env!!.set("coroutine", coroutine)
        if (!env!!.get("package")!!.isnil()) env!!.get("package")!!.get("loaded")!!.set("coroutine", coroutine)
        return coroutine
    }

    internal inner class create : LibFunction() {
        override fun call(f: LuaValue?): LuaValue? {
            return LuaThread((globals)!!, f!!.checkfunction())
        }
    }

    internal class resume : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val t: LuaThread = args.checkthread(1)
            return t.resume(args.subargs(2))
        }
    }

    internal inner class running : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs {
            val r: LuaThread = globals!!.running
            return (varargsOf(r, valueOf(r.isMainThread())))!!
        }
    }

    internal class status : LibFunction() {
        override fun call(t: LuaValue?): LuaValue? {
            val lt: LuaThread = t!!.checkthread()!!
            return valueOf(lt.status)
        }
    }

    internal inner class YieldFunction : VarArgFunction() {
        override fun invoke(args: Varargs?): Varargs {
            return globals!!.yield(args)
        }
    }

    internal inner class wrap : LibFunction() {
        override fun call(f: LuaValue?): LuaValue? {
            val func: LuaValue? = f!!.checkfunction()
            val thread: LuaThread = LuaThread((globals)!!, func)
            return net.blueva.luak.lib.CoroutineLib.wrapper(thread)
        }
    }

    internal class wrapper(luathread: LuaThread) : VarArgFunction() {
        val luathread: LuaThread

        init {
            this.luathread = luathread
        }

        override fun invoke(args: Varargs?): Varargs {
            val result: Varargs = luathread.resume(args)
            if (result.arg1()!!.toboolean()) {
                return (result.subargs(2))!!
            } else {
                return (error(result.arg(2)!!.tojstring()))!!
            }
        }
    }

    companion object {
        var coroutine_count: Int = 0
    }
}
