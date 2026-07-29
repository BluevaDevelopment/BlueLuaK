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
import net.blueva.luak.Lua
import net.blueva.luak.LuaError
import net.blueva.luak.LuaString
import net.blueva.luak.LuaTable
import net.blueva.luak.LuaThread
import net.blueva.luak.LuaValue
import net.blueva.luak.Varargs
import java.io.IOException
import java.io.InputStream

/**
 * Subclass of [LibFunction] which implements the lua basic library functions.
 * 
 * 
 * This contains all library functions listed as "basic functions" in the lua documentation for JME.
 * The functions dofile and loadfile use the
 * [Globals.finder] instance to find resource files.
 * Since JME has no file system by default, [BaseLib] implements
 * [ResourceFinder] using [Class.getResource],
 * which is the closest equivalent on JME.
 * The default loader chain in [PackageLib] will use these as well.
 * 
 * 
 * To use basic library functions that include a [ResourceFinder] based on
 * directory lookup, use [net.blueva.luak.lib.jse.JseBaseLib] instead.
 * 
 * 
 * Typically, this library is included as part of a call to either
 * [net.blueva.luak.lib.jse.JsePlatform.standardGlobals] or
 * [net.blueva.luak.lib.jme.JmePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals(); globals.get("print").call(LuaValue.valueOf("hello, world")); ` </pre>
 * 
 * 
 * For special cases where the smallest possible footprint is desired,
 * a minimal set of libraries could be loaded
 * directly via [Globals.load] using code such as:
 * <pre> `Globals globals = new Globals(); globals.load(new JseBaseLib()); globals.get("print").call(LuaValue.valueOf("hello, world")); ` </pre>
 * Doing so will ensure the library is properly initialized
 * and loaded into the globals table.
 * 
 * 
 * This is a direct port of the corresponding library in C.
 * @see net.blueva.luak.lib.jse.JseBaseLib
 * 
 * @see ResourceFinder
 * 
 * @see Globals.finder
 * 
 * @see LibFunction
 * 
 * @see net.blueva.luak.lib.jse.JsePlatform
 * 
 * @see net.blueva.luak.lib.jme.JmePlatform
 * 
 * @see [Lua 5.2 Base Lib Reference](http://www.lua.org/manual/5.2/manual.html.6.1)
 */
class BaseLib : TwoArgFunction(), ResourceFinder {
    var globals: Globals? = null


    /** Perform one-time initialization on the library by adding base functions
     * to the supplied environment, and returning it as the return value.
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env the environment to load into, which must be a Globals instance.
     */
    fun call(modname: LuaValue?, env: LuaValue): LuaValue {
        globals = env.checkglobals()
        globals.finder = this
        globals.baselib = this
        env.set("_G", env)
        env.set("_VERSION", Lua._VERSION)
        env.set("assert", net.blueva.luak.lib.BaseLib._assert())
        env.set("collectgarbage", net.blueva.luak.lib.BaseLib.collectgarbage())
        env.set("dofile", net.blueva.luak.lib.BaseLib.dofile())
        env.set("error", net.blueva.luak.lib.BaseLib.error())
        env.set("getmetatable", net.blueva.luak.lib.BaseLib.getmetatable())
        env.set("load", net.blueva.luak.lib.BaseLib.load())
        env.set("loadfile", net.blueva.luak.lib.BaseLib.loadfile())
        env.set("pcall", net.blueva.luak.lib.BaseLib.pcall())
        env.set("print", net.blueva.luak.lib.BaseLib.print(this))
        env.set("rawequal", net.blueva.luak.lib.BaseLib.rawequal())
        env.set("rawget", net.blueva.luak.lib.BaseLib.rawget())
        env.set("rawlen", net.blueva.luak.lib.BaseLib.rawlen())
        env.set("rawset", net.blueva.luak.lib.BaseLib.rawset())
        env.set("select", net.blueva.luak.lib.BaseLib.select())
        env.set("setmetatable", net.blueva.luak.lib.BaseLib.setmetatable())
        env.set("tonumber", net.blueva.luak.lib.BaseLib.tonumber())
        env.set("tostring", net.blueva.luak.lib.BaseLib.tostring())
        env.set("type", net.blueva.luak.lib.BaseLib.type())
        env.set("xpcall", net.blueva.luak.lib.BaseLib.xpcall())

        val next: next?
        env.set("next", net.blueva.luak.lib.BaseLib.next().also { next = it })
        env.set("pairs", net.blueva.luak.lib.BaseLib.pairs(next))
        env.set("ipairs", net.blueva.luak.lib.BaseLib.ipairs())

        return env
    }

    /** ResourceFinder implementation
     * 
     * Tries to open the file as a resource, which can work for JSE and JME.
     */
    fun findResource(filename: String): InputStream {
        return getClass().getResourceAsStream(if (filename.startsWith("/")) filename else "/" + filename)
    }


    // "assert", // ( v [,message] ) -> v, message | ERR
    internal class _assert : VarArgFunction() {
        fun invoke(args: Varargs): Varargs {
            if (!args.arg1().toboolean()) error(
                if (args.narg() > 1) args.optjstring(
                    2,
                    "assertion failed!"
                ) else "assertion failed!"
            )
            return args
        }
    }

    // "collectgarbage", // ( opt [,arg] ) -> value
    internal class collectgarbage : VarArgFunction() {
        fun invoke(args: Varargs): Varargs {
            val s: String? = args.optjstring(1, "collect")
            if ("collect".equals(s)) {
                System.gc()
                return ZERO
            } else if ("count".equals(s)) {
                val rt: Runtime = Runtime.getRuntime()
                val used: Long = rt.totalMemory() - rt.freeMemory()
                return varargsOf(valueOf(used / 1024.0), valueOf(used % 1024))
            } else if ("step".equals(s)) {
                System.gc()
                return LuaValue.TRUE
            } else {
                argerror(1, "invalid option '" + s + "'")
            }
            return NIL
        }
    }

    // "dofile", // ( filename ) -> result1, ...
    internal inner class dofile : VarArgFunction() {
        fun invoke(args: Varargs): Varargs {
            args.argcheck(args.isstring(1) || args.isnil(1), 1, "filename must be string or nil")
            val filename: String? = if (args.isstring(1)) args.tojstring(1) else null
            val v: Varargs = if (filename == null) loadStream(
                globals.STDIN,
                "=stdin",
                "bt",
                globals
            ) else loadFile(args.checkjstring(1), "bt", globals)
            return if (v.isnil(1)) error(v.tojstring(2)) else v.arg1().invoke()
        }
    }

    // "error", // ( message [,level] ) -> ERR
    internal class error : TwoArgFunction() {
        fun call(arg1: LuaValue, arg2: LuaValue): LuaValue? {
            if (arg1.isnil()) throw LuaError(NIL)
            if (!arg1.isstring() || arg2.optint(1) === 0) throw LuaError(arg1)
            throw LuaError(arg1.tojstring(), arg2.optint(1))
        }
    }

    // "getmetatable", // ( object ) -> table
    internal class getmetatable : LibFunction() {
        fun call(): LuaValue {
            return argerror(1, "value expected")
        }

        fun call(arg: LuaValue): LuaValue {
            val mt: LuaValue? = arg.getmetatable()
            return if (mt != null) mt.rawget(METATABLE).optvalue(mt) else NIL
        }
    }

    // "load", // ( ld [, source [, mode [, env]]] ) -> chunk | nil, msg
    internal inner class load : VarArgFunction() {
        fun invoke(args: Varargs): Varargs {
            val ld: LuaValue = args.arg1()
            if (!ld.isstring() && !ld.isfunction()) {
                throw LuaError("bad argument #1 to 'load' (string or function expected, got " + ld.typename() + ")")
            }
            val source: String? = args.optjstring(2, if (ld.isstring()) ld.tojstring() else "=(load)")
            val mode: String? = args.optjstring(3, "bt")
            val env: LuaValue? = args.optvalue(4, globals)
            return loadStream(
                if (ld.isstring()) ld.strvalue()
                    .toInputStream() else net.blueva.luak.lib.BaseLib.StringInputStream(ld.checkfunction()),
                source,
                mode,
                env
            )
        }
    }

    // "loadfile", // ( [filename [, mode [, env]]] ) -> chunk | nil, msg
    internal inner class loadfile : VarArgFunction() {
        fun invoke(args: Varargs): Varargs? {
            args.argcheck(args.isstring(1) || args.isnil(1), 1, "filename must be string or nil")
            val filename: String? = if (args.isstring(1)) args.tojstring(1) else null
            val mode: String? = args.optjstring(2, "bt")
            val env: LuaValue? = args.optvalue(3, globals)
            return if (filename == null) loadStream(globals.STDIN, "=stdin", mode, env) else loadFile(
                filename,
                mode,
                env
            )
        }
    }

    // "pcall", // (f, arg1, ...) -> status, result1, ...
    internal inner class pcall : VarArgFunction() {
        fun invoke(args: Varargs): Varargs {
            val func: LuaValue = args.checkvalue(1)
            if (globals != null && globals.debuglib != null) globals.debuglib.onCall(this)
            try {
                return varargsOf(TRUE, func.invoke(args.subargs(2)))
            } catch (le: LuaError) {
                val m: LuaValue? = le.getMessageObject()
                return varargsOf(FALSE, if (m != null) m else NIL)
            } catch (e: Exception) {
                val m: String? = e.getMessage()
                return varargsOf(FALSE, valueOf(if (m != null) m else e.toString()))
            } finally {
                if (globals != null && globals.debuglib != null) globals.debuglib.onReturn()
            }
        }
    }

    // "print", // (...) -> void
    internal inner class print(val baselib: BaseLib) : VarArgFunction() {
        fun invoke(args: Varargs): Varargs {
            val tostring: LuaValue = globals.get("tostring")
            var i = 1
            val n: Int = args.narg()
            while (i <= n) {
                if (i > 1) globals.STDOUT.print('\t')
                val s: LuaString = tostring.call(args.arg(i)).strvalue()
                globals.STDOUT.print(s.tojstring())
                i++
            }
            globals.STDOUT.print('\n')
            return NONE
        }
    }


    // "rawequal", // (v1, v2) -> boolean
    internal class rawequal : LibFunction() {
        fun call(): LuaValue {
            return argerror(1, "value expected")
        }

        fun call(arg: LuaValue?): LuaValue {
            return argerror(2, "value expected")
        }

        fun call(arg1: LuaValue, arg2: LuaValue?): LuaValue {
            return valueOf(arg1.raweq(arg2))
        }
    }

    // "rawget", // (table, index) -> value
    internal class rawget : TableLibFunction() {
        fun call(arg: LuaValue?): LuaValue {
            return argerror(2, "value expected")
        }

        fun call(arg1: LuaValue, arg2: LuaValue?): LuaValue {
            return arg1.checktable().rawget(arg2)
        }
    }


    // "rawlen", // (v) -> value
    internal class rawlen : LibFunction() {
        fun call(arg: LuaValue): LuaValue {
            return valueOf(arg.rawlen())
        }
    }

    // "rawset", // (table, index, value) -> table
    internal class rawset : TableLibFunction() {
        fun call(table: LuaValue?): LuaValue {
            return argerror(2, "value expected")
        }

        fun call(table: LuaValue?, index: LuaValue?): LuaValue {
            return argerror(3, "value expected")
        }

        fun call(table: LuaValue, index: LuaValue, value: LuaValue?): LuaValue {
            val t: LuaTable = table.checktable()
            if (!index.isvalidkey()) argerror(2, "table index is nil")
            t.rawset(index, value)
            return t
        }
    }

    // "select", // (f, ...) -> value1, ...
    internal class select : VarArgFunction() {
        fun invoke(args: Varargs): Varargs {
            val n: Int = args.narg() - 1
            if (args.arg1().equals(valueOf("#"))) return valueOf(n)
            val i: Int = args.checkint(1)
            if (i == 0 || i < -n) argerror(1, "index out of range")
            return args.subargs(if (i < 0) n + i + 2 else i + 1)
        }
    }

    // "setmetatable", // (table, metatable) -> table
    internal class setmetatable : TableLibFunction() {
        fun call(table: LuaValue?): LuaValue {
            return argerror(2, "nil or table expected")
        }

        fun call(table: LuaValue, metatable: LuaValue): LuaValue {
            val mt0: LuaValue? = table.checktable().getmetatable()
            if (mt0 != null && !mt0.rawget(METATABLE).isnil()) error("cannot change a protected metatable")
            return table.setmetatable(if (metatable.isnil()) null else metatable.checktable())
        }
    }

    // "tonumber", // (e [,base]) -> value
    internal class tonumber : LibFunction() {
        fun call(e: LuaValue): LuaValue {
            return e.tonumber()
        }

        fun call(e: LuaValue, base: LuaValue): LuaValue {
            if (base.isnil()) return e.tonumber()
            val b: Int = base.checkint()
            if (b < 2 || b > 36) argerror(2, "base out of range")
            return e.checkstring().tonumber(b)
        }
    }

    // "tostring", // (e) -> value
    internal class tostring : LibFunction() {
        fun call(arg: LuaValue): LuaValue {
            val h: LuaValue = arg.metatag(TOSTRING)
            if (!h.isnil()) return h.call(arg)
            val v: LuaValue = arg.tostring()
            if (!v.isnil()) return v
            return valueOf(arg.tojstring())
        }
    }

    // "type",  // (v) -> value
    internal class type : LibFunction() {
        fun call(arg: LuaValue): LuaValue {
            return valueOf(arg.typename())
        }
    }

    // "xpcall", // (f, err) -> result1, ...
    internal inner class xpcall : VarArgFunction() {
        fun invoke(args: Varargs): Varargs {
            val t: LuaThread = globals.running
            val preverror: LuaValue? = t.errorfunc
            t.errorfunc = args.checkvalue(2)
            try {
                if (globals != null && globals.debuglib != null) globals.debuglib.onCall(this)
                try {
                    return varargsOf(TRUE, args.arg1().invoke(args.subargs(3)))
                } catch (le: LuaError) {
                    val m: LuaValue? = le.getMessageObject()
                    return varargsOf(FALSE, if (m != null) m else NIL)
                } catch (e: Exception) {
                    val m: String? = e.getMessage()
                    return varargsOf(FALSE, valueOf(if (m != null) m else e.toString()))
                } finally {
                    if (globals != null && globals.debuglib != null) globals.debuglib.onReturn()
                }
            } finally {
                t.errorfunc = preverror
            }
        }
    }

    // "pairs" (t) -> iter-func, t, nil
    internal class pairs(val next: BaseLib.next) : VarArgFunction() {
        fun invoke(args: Varargs): Varargs {
            return varargsOf(next, args.checktable(1), NIL)
        }
    }

    // // "ipairs", // (t) -> iter-func, t, 0
    internal class ipairs : VarArgFunction() {
        var inext: inext = net.blueva.luak.lib.BaseLib.inext()
        fun invoke(args: Varargs): Varargs {
            return varargsOf(inext, args.checktable(1), ZERO)
        }
    }

    // "next"  ( table, [index] ) -> next-index, next-value
    internal class next : VarArgFunction() {
        fun invoke(args: Varargs): Varargs {
            return args.checktable(1).next(args.arg(2))
        }
    }

    // "inext" ( table, [int-index] ) -> next-index, next-value
    internal class inext : VarArgFunction() {
        fun invoke(args: Varargs): Varargs {
            return args.checktable(1).inext(args.arg(2))
        }
    }

    /**
     * Load from a named file, returning the chunk or nil,error of can't load
     * @param env
     * @param mode
     * @return Varargs containing chunk, or NIL,error-text on error
     */
    fun loadFile(filename: String?, mode: String?, env: LuaValue?): Varargs {
        val `is`: InputStream? = globals.finder.findResource(filename)
        if (`is` == null) return varargsOf(NIL, valueOf("cannot open " + filename + ": No such file or directory"))
        try {
            return loadStream(`is`, "@" + filename, mode, env)
        } finally {
            try {
                `is`.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadStream(`is`: InputStream?, chunkname: String?, mode: String?, env: LuaValue?): Varargs {
        try {
            if (`is` == null) return varargsOf(NIL, valueOf("not found: " + chunkname))
            return globals.load(`is`, chunkname, mode, env)
        } catch (e: Exception) {
            return varargsOf(NIL, valueOf(e.getMessage()))
        }
    }


    private class StringInputStream(func: LuaValue) : InputStream() {
        val func: LuaValue
        var bytes: ByteArray
        var offset: Int = 0
        var remaining: Int = 0

        init {
            this.func = func
        }

        @kotlin.Throws(IOException::class)
        fun read(): Int {
            if (remaining < 0) return -1
            if (remaining == 0) {
                val s: LuaValue = func.call()
                if (s.isnil()) return (-1).also { remaining = it }
                val ls: LuaString = s.strvalue()
                bytes = ls.m_bytes
                offset = ls.m_offset
                remaining = ls.m_length
                if (remaining <= 0) return -1
            }
            --remaining
            return 0xFF and bytes[offset++].toInt()
        }
    }
}
