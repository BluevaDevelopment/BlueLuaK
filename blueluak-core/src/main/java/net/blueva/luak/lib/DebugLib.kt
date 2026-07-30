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
import net.blueva.luak.LuaBoolean
import net.blueva.luak.LuaClosure
import net.blueva.luak.LuaError
import net.blueva.luak.LuaFunction
import net.blueva.luak.LuaNil
import net.blueva.luak.LuaNumber
import net.blueva.luak.LuaString
import net.blueva.luak.LuaTable
import net.blueva.luak.LuaThread
import net.blueva.luak.LuaUserdata
import net.blueva.luak.LuaValue
import net.blueva.luak.Print
import net.blueva.luak.Prototype
import net.blueva.luak.Varargs

/**
 * Subclass of [LibFunction] which implements the lua standard `debug`
 * library.
 * 
 * 
 * The debug library in luaj tries to emulate the behavior of the corresponding C-based lua library.
 * To do this, it must maintain a separate stack of calls to [LuaClosure] and [LibFunction]
 * instances.
 * Especially when lua-to-java bytecode compiling is being used
 * via a [net.blueva.luak.Globals.Compiler] such as [net.blueva.luak.luajc.LuaJC],
 * this cannot be done in all cases.
 * 
 * 
 * Typically, this library is included as part of a call to either
 * [net.blueva.luak.lib.jse.JsePlatform.debugGlobals] or
 * [net.blueva.luak.lib.jme.JmePlatform.debugGlobals]
 * <pre> `Globals globals = JsePlatform.debugGlobals(); System.out.println( globals.get("debug").get("traceback").call() ); ` </pre>
 * 
 * 
 * To instantiate and use it directly,
 * link it into your globals table via [LuaValue.load] using code such as:
 * <pre> `Globals globals = new Globals(); globals.load(new JseBaseLib()); globals.load(new PackageLib()); globals.load(new DebugLib()); System.out.println( globals.get("debug").get("traceback").call() ); ` </pre>
 * 
 * 
 * This library exposes the entire state of lua code, and provides method to see and modify
 * all underlying lua values within a Java VM so should not be exposed to client code
 * in a shared server environment.
 * 
 * @see LibFunction
 * 
 * @see net.blueva.luak.lib.jse.JsePlatform
 * 
 * @see net.blueva.luak.lib.jme.JmePlatform
 * 
 * @see [Lua 5.2 Debug Lib Reference](http://www.lua.org/manual/5.2/manual.html.6.10)
 */
class DebugLib : TwoArgFunction() {
    var globals: Globals? = null

    /** Perform one-time initialization on the library by creating a table
     * containing the library functions, adding that table to the supplied environment,
     * adding the table to package.loaded, and returning table as the return value.
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env the environment to load into, which must be a Globals instance.
     */
    override fun call(modname: LuaValue?, env: LuaValue?): LuaValue? {
        globals = env!!.checkglobals()
        globals!!.debuglib = this
        val debug: LuaTable = LuaTable()
        debug.set("debug", net.blueva.luak.lib.DebugLib.debug())
        debug.set("gethook", gethook())
        debug.set("getinfo", getinfo())
        debug.set("getlocal", getlocal())
        debug.set("getmetatable", net.blueva.luak.lib.DebugLib.getmetatable())
        debug.set("getregistry", getregistry())
        debug.set("getupvalue", net.blueva.luak.lib.DebugLib.getupvalue())
        debug.set("getuservalue", net.blueva.luak.lib.DebugLib.getuservalue())
        debug.set("sethook", sethook())
        debug.set("setlocal", setlocal())
        debug.set("setmetatable", net.blueva.luak.lib.DebugLib.setmetatable())
        debug.set("setupvalue", net.blueva.luak.lib.DebugLib.setupvalue())
        debug.set("setuservalue", net.blueva.luak.lib.DebugLib.setuservalue())
        debug.set("traceback", traceback())
        debug.set("upvalueid", net.blueva.luak.lib.DebugLib.upvalueid())
        debug.set("upvaluejoin", net.blueva.luak.lib.DebugLib.upvaluejoin())
        env!!.set("debug", debug)
        if (!env!!.get("package")!!.isnil()) env!!.get("package")!!.get("loaded")!!.set("debug", debug)
        return debug
    }

    // debug.debug()
    internal class debug : ZeroArgFunction() {
        override fun call(): LuaValue? {
            return (NONE)!!
        }
    }

    // debug.gethook ([thread])
    internal inner class gethook : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val t: LuaThread = if (args.narg() > 0) args.checkthread(1) else globals!!.running
            val s: LuaThread.State = t.state
            return varargsOf(
                if (s.hookfunc != null) s.hookfunc else NIL,
                valueOf((if (s.hookcall) "c" else "") + (if (s.hookline) "l" else "") + (if (s.hookrtrn) "r" else "")),
                valueOf(s.hookcount)
            )
        }
    }

    //	debug.getinfo ([thread,] f [, what])
    internal inner class getinfo : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            var a = 1
            val thread: LuaThread = if (args.isthread(a)) args.checkthread(a++) else globals!!.running
            var func: LuaValue? = args.arg(a++)
            val what: String = args.optjstring(a++, "flnStu")
            val callstack = callstack(thread)

            // find the stack info
            val frame: CallFrame?
            if (func!!.isnumber()) {
                frame = callstack.getCallFrame(func!!.toint())
                if (frame == null) return (NONE)!!
                func = frame.f
            } else if (func!!.isfunction()) {
                frame = callstack.findCallFrame(func)
            } else {
                return (argerror(a - 2, "function or level"))!!
            }

            // start a table
            val ar = callstack.auxgetinfo(what, func as LuaFunction?, frame)
            val info: LuaTable = LuaTable()
            if (what.indexOf('S') >= 0) {
                info.set(net.blueva.luak.lib.DebugLib.Companion.WHAT, net.blueva.luak.lib.DebugLib.Companion.LUA)
                info.set(net.blueva.luak.lib.DebugLib.Companion.SOURCE, valueOf(ar.source))
                info.set(net.blueva.luak.lib.DebugLib.Companion.SHORT_SRC, valueOf(ar.short_src))
                info.set(net.blueva.luak.lib.DebugLib.Companion.LINEDEFINED, valueOf(ar.linedefined))
                info.set(net.blueva.luak.lib.DebugLib.Companion.LASTLINEDEFINED, valueOf(ar.lastlinedefined))
            }
            if (what.indexOf('l') >= 0) {
                info.set(net.blueva.luak.lib.DebugLib.Companion.CURRENTLINE, valueOf(ar.currentline))
            }
            if (what.indexOf('u') >= 0) {
                info.set(net.blueva.luak.lib.DebugLib.Companion.NUPS, valueOf(ar.nups))
                info.set(net.blueva.luak.lib.DebugLib.Companion.NPARAMS, valueOf(ar.nparams))
                info.set(net.blueva.luak.lib.DebugLib.Companion.ISVARARG, if (ar.isvararg) ONE else ZERO)
            }
            if (what.indexOf('n') >= 0) {
                info.set(
                    net.blueva.luak.lib.DebugLib.Companion.NAME,
                    LuaValue.valueOf(if (ar.name != null) ar.name else "?")
                )
                info.set(net.blueva.luak.lib.DebugLib.Companion.NAMEWHAT, LuaValue.valueOf(ar.namewhat))
            }
            if (what.indexOf('t') >= 0) {
                info.set(net.blueva.luak.lib.DebugLib.Companion.ISTAILCALL, ZERO)
            }
            if (what.indexOf('L') >= 0) {
                val lines: LuaTable = LuaTable()
                info.set(net.blueva.luak.lib.DebugLib.Companion.ACTIVELINES, lines)
                var cf: CallFrame?
                var l = 1
                while ((callstack.getCallFrame(l).also { cf = it }) != null) {
                    if (cf!!.f === func) lines.insert(-1, valueOf(cf.currentline()))
                    ++l
                }
            }
            if (what.indexOf('f') >= 0) {
                if (func != null) info.set(net.blueva.luak.lib.DebugLib.Companion.FUNC, func)
            }
            return info
        }
    }

    //	debug.getlocal ([thread,] f, local)
    internal inner class getlocal : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            var a = 1
            val thread: LuaThread = if (args.isthread(a)) args.checkthread(a++) else globals!!.running
            val level: Int = args.checkint(a++)
            val local: Int = args.checkint(a++)
            val f = callstack(thread).getCallFrame(level)
            return (if (f != null) f.getLocal(local) else NONE)!!
        }
    }

    //	debug.getmetatable (value)
    internal class getmetatable : LibFunction() {
        override fun call(v: LuaValue?): LuaValue? {
            val mt: LuaValue? = v!!.getmetatable()
            return if (mt != null) mt else NIL
        }
    }

    //	debug.getregistry ()
    internal inner class getregistry : ZeroArgFunction() {
        override fun call(): LuaValue? {
            return (globals)!!
        }
    }

    //	debug.getupvalue (f, up)
    internal class getupvalue : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val func: LuaValue? = args.checkfunction(1)
            val up: Int = args.checkint(2)
            if (func is LuaClosure) {
                val c: LuaClosure = func as LuaClosure
                val name: LuaString? = net.blueva.luak.lib.DebugLib.Companion.findupvalue(c, up)
                if (name != null) {
                    return (varargsOf(name, (c.upValues[up - 1]!!.getValue())!!))!!
                }
            }
            return NIL
        }
    }

    //	debug.getuservalue (u)
    internal class getuservalue : LibFunction() {
        override fun call(u: LuaValue?): LuaValue? {
            return if (u!!.isuserdata()) u else NIL
        }
    }


    // debug.sethook ([thread,] hook, mask [, count])
    internal inner class sethook : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            var a = 1
            val t: LuaThread = if (args.isthread(a)) args.checkthread(a++) else globals!!.running
            val func: LuaValue? = args.optfunction(a++, null)
            val str: String = args.optjstring(a++, "")
            val count: Int = args.optint(a++, 0)
            var call = false
            var line = false
            var rtrn = false
            for (i in 0..<str.length) when (str[i]) {
                'c' -> call = true
                'l' -> line = true
                'r' -> rtrn = true
            }
            val s: LuaThread.State = t.state
            s.hookfunc = func
            s.hookcall = call
            s.hookline = line
            s.hookcount = count
            s.hookrtrn = rtrn
            return (NONE)!!
        }
    }

    //	debug.setlocal ([thread,] level, local, value)
    internal inner class setlocal : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            var a = 1
            val thread: LuaThread = if (args.isthread(a)) args.checkthread(a++) else globals!!.running
            val level: Int = args.checkint(a++)
            val local: Int = args.checkint(a++)
            val value: LuaValue? = args.arg(a++)
            val f = callstack(thread).getCallFrame(level)
            return (if (f != null) f.setLocal(local, value) else NONE)!!
        }
    }

    //	debug.setmetatable (value, table)
    internal class setmetatable : TwoArgFunction() {
        override fun call(value: LuaValue?, table: LuaValue?): LuaValue? {
            val mt: LuaValue? = table!!.opttable(null)
            when (value!!.type()) {
                TNIL -> LuaNil.s_metatable = mt
                TNUMBER -> LuaNumber.s_metatable = mt
                TBOOLEAN -> LuaBoolean.s_metatable = mt
                TSTRING -> LuaString.s_metatable = mt
                TFUNCTION -> LuaFunction.s_metatable = mt
                TTHREAD -> LuaThread.s_metatable = mt
                else -> value!!.setmetatable(mt)
            }
            return value
        }
    }

    //	debug.setupvalue (f, up, value)
    internal class setupvalue : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val func: LuaValue? = args.checkfunction(1)
            val up: Int = args.checkint(2)
            val value: LuaValue? = args.arg(3)
            if (func is LuaClosure) {
                val c: LuaClosure = func as LuaClosure
                val name: LuaString? = net.blueva.luak.lib.DebugLib.Companion.findupvalue(c, up)
                if (name != null) {
                    c.upValues[up - 1]!!.setValue(value)
                    return name
                }
            }
            return NIL
        }
    }

    //	debug.setuservalue (udata, value)
    internal class setuservalue : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val o: Object? = args.checkuserdata(1)
            val v: LuaValue = args.checkvalue(2)!!
            val u: LuaUserdata = args.arg1() as LuaUserdata
            u.m_instance = v.checkuserdata()
            u.m_metatable = v.getmetatable()
            return (NONE)!!
        }
    }

    //	debug.traceback ([thread,] [message [, level]])
    internal inner class traceback : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            var a = 1
            val thread: LuaThread = if (args.isthread(a)) args.checkthread(a++) else globals!!.running
            val message: String? = args.optjstring(a++, null)
            val level: Int = args.optint(a++, 1)
            val tb = callstack(thread).traceback(level)
            return valueOf(if (message != null) message.toString() + "\n" + tb else tb)
        }
    }

    //	debug.upvalueid (f, n)
    internal class upvalueid : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val func: LuaValue? = args.checkfunction(1)
            val up: Int = args.checkint(2)
            if (func is LuaClosure) {
                val c: LuaClosure = func as LuaClosure
                if (c.upValues != null && up > 0 && up <= c.upValues.size) {
                    return valueOf(c.upValues[up - 1].hashCode())
                }
            }
            return NIL
        }
    }

    //	debug.upvaluejoin (f1, n1, f2, n2)
    internal class upvaluejoin : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val f1: LuaClosure = args.checkclosure(1)
            val n1: Int = args.checkint(2)
            val f2: LuaClosure = args.checkclosure(3)
            val n2: Int = args.checkint(4)
            if (n1 < 1 || n1 > f1.upValues.size) argerror("index out of range")
            if (n2 < 1 || n2 > f2.upValues.size) argerror("index out of range")
            f1.upValues[n1 - 1] = f2.upValues[n2 - 1]
            return (NONE)!!
        }
    }

    fun onCall(f: LuaFunction?) {
        val s: LuaThread.State = globals!!.running.state
        if (s.inhook) return
        callstack().onCall(f)
        if (s.hookcall) callHook(s, net.blueva.luak.lib.DebugLib.Companion.CALL, NIL)
    }

    fun onCall(c: LuaClosure?, varargs: Varargs?, stack: Array<LuaValue?>?) {
        val s: LuaThread.State = globals!!.running.state
        if (s.inhook) return
        callstack().onCall(c, varargs, stack)
        if (s.hookcall) callHook(s, net.blueva.luak.lib.DebugLib.Companion.CALL, NIL)
    }

    fun onInstruction(pc: Int, v: Varargs?, top: Int) {
        val s: LuaThread.State = globals!!.running.state
        if (s.inhook) return
        callstack().onInstruction(pc, v, top)
        if (s.hookfunc == null) return
        if (s.hookcount > 0) if (++s.bytecodes % s.hookcount === 0) callHook(
            s,
            net.blueva.luak.lib.DebugLib.Companion.COUNT,
            NIL
        )
        if (s.hookline) {
            val newline = callstack().currentline()
            if (newline != s.lastline) {
                s.lastline = newline
                callHook(s, net.blueva.luak.lib.DebugLib.Companion.LINE, LuaValue.valueOf(newline))
            }
        }
    }

    fun onReturn() {
        val s: LuaThread.State = globals!!.running.state
        if (s.inhook) return
        callstack().onReturn()
        if (s.hookrtrn) callHook(s, net.blueva.luak.lib.DebugLib.Companion.RETURN, NIL)
    }

    fun traceback(level: Int): String {
        return callstack().traceback(level)
    }

    fun getCallFrame(level: Int): CallFrame? {
        return callstack().getCallFrame(level)
    }

    fun callHook(s: LuaThread.State, type: LuaValue?, arg: LuaValue?) {
        if (s.inhook || s.hookfunc == null) return
        s.inhook = true
        try {
            s.hookfunc!!.call(type, arg)
        } catch (e: LuaError) {
            throw e
        } catch (e: RuntimeException) {
            throw LuaError(e)
        } finally {
            s.inhook = false
        }
    }

    @kotlin.jvm.JvmOverloads
    fun callstack(t: LuaThread = globals!!.running): CallStack {
        if (t.callstack == null) t.callstack = net.blueva.luak.lib.DebugLib.CallStack()
        return t.callstack as CallStack
    }

    class DebugInfo {
        var name: String? = null /* (n) */
        var namewhat: String? = null /* (n) 'global', 'local', 'field', 'method' */
        var what: String? = null /* (S) 'Lua', 'C', 'main', 'tail' */
        var source: String? = null /* (S) */
        var currentline: Int = 0 /* (l) */
        var linedefined: Int = 0 /* (S) */
        var lastlinedefined: Int = 0 /* (S) */
        var nups: Short = 0 /* (u) number of upvalues */
        var nparams: Short = 0 /* (u) number of parameters */
        var isvararg: Boolean = false /* (u) */
        var istailcall: Boolean = false /* (t) */
        var short_src: String? = null /* (S) */
        var cf: CallFrame? = null /* active function */

        fun funcinfo(f: LuaFunction) {
            if (f.isclosure()) {
                val p: Prototype = f.checkclosure()!!.p
                this.source = if (p.source != null) p.source!!.tojstring() else "=?"
                this.linedefined = p.linedefined
                this.lastlinedefined = p.lastlinedefined
                this.what = if (this.linedefined == 0) "main" else "Lua"
                this.short_src = p.shortsource()
            } else {
                this.source = "=[Java]"
                this.linedefined = -1
                this.lastlinedefined = -1
                this.what = "Java"
                this.short_src = f.name()
            }
        }
    }

    class CallStack internal constructor() {
        var frame: Array<CallFrame?>? = net.blueva.luak.lib.DebugLib.CallStack.Companion.EMPTY
        var calls: Int = 0

        @kotlin.jvm.Synchronized
        fun currentline(): Int {
            return if (calls > 0) frame!![calls - 1]!!.currentline() else -1
        }

        @kotlin.jvm.Synchronized
        private fun pushcall(): CallFrame? {
            if (calls >= frame!!.size) {
                val n: Int = Math.max(4, frame!!.size * 3 / 2)
                val f = arrayOfNulls<CallFrame>(n)
                System.arraycopy(frame, 0, f, 0, frame!!.size)
                for (i in frame!!.size..<n) f[i] = net.blueva.luak.lib.DebugLib.CallFrame()
                frame = f
                for (i in 1..<n) f[i]!!.previous = f[i - 1]
            }
            return frame!![calls++]
        }

        @kotlin.jvm.Synchronized
        fun onCall(function: LuaFunction?) {
            pushcall()!!.set(function)
        }

        @kotlin.jvm.Synchronized
        fun onCall(function: LuaClosure?, varargs: Varargs?, stack: Array<LuaValue?>?) {
            pushcall()!!.set(function, varargs, stack)
        }

        @kotlin.jvm.Synchronized
        fun onReturn() {
            if (calls > 0) frame!![--calls]!!.reset()
        }

        @kotlin.jvm.Synchronized
        fun onInstruction(pc: Int, v: Varargs?, top: Int) {
            if (calls > 0) frame!![calls - 1]!!.instr(pc, v, top)
        }

        /**
         * Get the traceback starting at a specific level.
         * @param level
         * @return String containing the traceback.
         */
        @kotlin.jvm.Synchronized
        fun traceback(level: Int): String {
            var level = level
            val sb: StringBuffer = StringBuffer()
            sb.append("stack traceback:")
            var c: CallFrame?
            while ((getCallFrame(level++).also { c = it }) != null) {
                sb.append("\n\t")
                sb.append(c!!.shortsource())
                sb.append(':')
                if (c.currentline() > 0) sb.append(c.currentline().toString() + ":")
                sb.append(" in ")
                val ar = auxgetinfo("n", c.f, c)
                if (c.linedefined() == 0) sb.append("main chunk")
                else if (ar.name != null) {
                    sb.append("function '")
                    sb.append(ar.name)
                    sb.append('\'')
                } else {
                    sb.append("function <")
                    sb.append(c.shortsource())
                    sb.append(':')
                    sb.append(c.linedefined())
                    sb.append('>')
                }
            }
            sb.append("\n\t[Java]: in ?")
            return sb.toString()
        }

        @kotlin.jvm.Synchronized
        fun getCallFrame(level: Int): CallFrame? {
            if (level < 1 || level > calls) return null
            return frame!![calls - level]
        }

        @kotlin.jvm.Synchronized
        fun findCallFrame(func: LuaValue?): CallFrame? {
            for (i in 1..calls) if (frame!![calls - i]!!.f === func) return frame!![i]
            return null
        }


        @kotlin.jvm.Synchronized
        fun auxgetinfo(what: String, f: LuaFunction?, ci: CallFrame?): DebugInfo {
            val ar: DebugInfo = net.blueva.luak.lib.DebugLib.DebugInfo()
            var i = 0
            val n: Int = what.length()
            while (i < n) {
                when (what[i]) {
                    'S' -> ar.funcinfo((f)!!)
                    'l' -> ar.currentline = if (ci != null && ci.f!!.isclosure()) ci.currentline() else -1
                    'u' -> if (f != null && f.isclosure()) {
                        val p: Prototype = f.checkclosure()!!.p
                        ar.nups = p.upvalues!!.size as Short
                        ar.nparams = p.numparams as Short
                        ar.isvararg = p.is_vararg !== 0
                    } else {
                        ar.nups = 0
                        ar.isvararg = true
                        ar.nparams = 0
                    }

                    't' -> ar.istailcall = false
                    'n' -> {
                        /* calling function is a known Lua function? */
                        if (ci != null && ci.previous != null) {
                            if (ci.previous!!.f!!.isclosure()) {
                                val nw: NameWhat? = net.blueva.luak.lib.DebugLib.Companion.getfuncname(ci.previous!!)
                                if (nw != null) {
                                    ar.name = nw.name
                                    ar.namewhat = nw.namewhat
                                }
                            }
                        }
                        if (ar.namewhat == null) {
                            ar.namewhat = "" /* not found */
                            ar.name = null
                        }
                    }

                    'L', 'f' -> {}
                    else -> {}
                }
                ++i
            }
            return ar
        }

        companion object {
            val EMPTY: Array<CallFrame?> = arrayOf<CallFrame?>()
        }
    }

    class CallFrame {
        var f: LuaFunction? = null
        var pc: Int = 0
        var top: Int = 0
        var v: Varargs? = null
        var stack: Array<LuaValue?>?
        var previous: CallFrame? = null
        fun set(function: LuaClosure?, varargs: Varargs?, stack: Array<LuaValue?>?) {
            this.f = function
            this.v = varargs
            this.stack = stack
        }

        fun shortsource(): String? {
            return if (f!!.isclosure()) f!!.checkclosure()!!.p.shortsource() else "[Java]"
        }

        fun set(function: LuaFunction?) {
            this.f = function
        }

        fun reset() {
            this.f = null
            this.v = null
            this.stack = null
        }

        fun instr(pc: Int, v: Varargs?, top: Int) {
            this.pc = pc
            this.v = v
            this.top = top
            if (net.blueva.luak.lib.DebugLib.Companion.TRACE) Print.printState((f!!.checkclosure())!!, pc, stack, top, v)
        }

        fun getLocal(i: Int): Varargs {
            val name: LuaString? = getlocalname(i)
            if (i >= 1 && i <= stack!!.size && stack!![i - 1] != null) return varargsOf(
                if (name == null) NIL else name,
                stack!![i - 1]
            )!!
            else return NIL
        }

        fun setLocal(i: Int, value: LuaValue?): Varargs? {
            val name: LuaString? = getlocalname(i)
            if (i >= 1 && i <= stack!!.size && stack!![i - 1] != null) {
                stack!![i - 1] = value
                return if (name == null) NIL else name
            } else {
                return NIL
            }
        }

        fun currentline(): Int {
            if (!f!!.isclosure()) return -1
            val li: IntArray? = f!!.checkclosure()!!.p.lineinfo
            return if (li == null || pc < 0 || pc >= li.size) -1 else li[pc]
        }

        fun sourceline(): String? {
            if (!f!!.isclosure()) return f!!.tojstring()
            return f!!.checkclosure()!!.p.shortsource() + ":" + currentline()
        }

        fun linedefined(): Int {
            return if (f!!.isclosure()) f!!.checkclosure()!!.p.linedefined else -1
        }

        fun getlocalname(index: Int): LuaString? {
            if (!f!!.isclosure()) return null
            return f!!.checkclosure()!!.p.getlocalname(index, pc)
        }
    }

    class NameWhat(val name: String, val namewhat: String)

    companion object {
        var CALLS: Boolean = false
        var TRACE: Boolean = false

        init {
            try {
                net.blueva.luak.lib.DebugLib.Companion.CALLS = (null != System.getProperty("CALLS"))
            } catch (e: Exception) {
            }
            try {
                net.blueva.luak.lib.DebugLib.Companion.TRACE = (null != System.getProperty("TRACE"))
            } catch (e: Exception) {
            }
        }

        val LUA: LuaString? = valueOf("Lua")
        private val QMARK: LuaString? = valueOf("?")
        private val CALL: LuaString? = valueOf("call")
        private val LINE: LuaString? = valueOf("line")
        private val COUNT: LuaString? = valueOf("count")
        private val RETURN: LuaString? = valueOf("return")

        val FUNC: LuaString? = valueOf("func")
        val ISTAILCALL: LuaString? = valueOf("istailcall")
        val ISVARARG: LuaString? = valueOf("isvararg")
        val NUPS: LuaString? = valueOf("nups")
        val NPARAMS: LuaString? = valueOf("nparams")
        val NAME: LuaString? = valueOf("name")
        val NAMEWHAT: LuaString? = valueOf("namewhat")
        val WHAT: LuaString? = valueOf("what")
        val SOURCE: LuaString? = valueOf("source")
        val SHORT_SRC: LuaString? = valueOf("short_src")
        val LINEDEFINED: LuaString? = valueOf("linedefined")
        val LASTLINEDEFINED: LuaString? = valueOf("lastlinedefined")
        val CURRENTLINE: LuaString? = valueOf("currentline")
        val ACTIVELINES: LuaString? = valueOf("activelines")

        fun findupvalue(c: LuaClosure, up: Int): LuaString? {
            if (c.upValues != null && up > 0 && up <= c.upValues.size) {
                if (c.p.upvalues != null && up <= c.p.upvalues!!.size) return c.p.upvalues!![up - 1]!!.name
                else return LuaString.valueOf("." + up)
            }
            return null
        }

        fun lua_assert(x: Boolean) {
            if (!x) throw RuntimeException("lua_assert failed")
        }

        // Return the name info if found, or null if no useful information could be found.
        fun getfuncname(frame: CallFrame): NameWhat? {
            if (!frame.f!!.isclosure()) return net.blueva.luak.lib.DebugLib.NameWhat(frame.f!!.classnamestub(), "Java")
            val p: Prototype = frame.f!!.checkclosure()!!.p
            val pc = frame.pc
            val i: Int = p.code!![pc] /* calling instruction */
            val tm: LuaString
            when (Lua.GET_OPCODE(i)) {
                Lua.OP_CALL, Lua.OP_TAILCALL -> return net.blueva.luak.lib.DebugLib.Companion.getobjname(
                    p,
                    pc,
                    Lua.GETARG_A(i)
                )

                Lua.OP_TFORCALL -> return net.blueva.luak.lib.DebugLib.NameWhat("(for iterator)", "(for iterator")
                Lua.OP_SELF, Lua.OP_GETTABUP, Lua.OP_GETTABLE -> tm = LuaValue.INDEX
                Lua.OP_SETTABUP, Lua.OP_SETTABLE -> tm = LuaValue.NEWINDEX
                Lua.OP_EQ -> tm = LuaValue.EQ
                Lua.OP_ADD -> tm = LuaValue.ADD
                Lua.OP_SUB -> tm = LuaValue.SUB
                Lua.OP_MUL -> tm = LuaValue.MUL
                Lua.OP_DIV -> tm = LuaValue.DIV
                Lua.OP_MOD -> tm = LuaValue.MOD
                Lua.OP_POW -> tm = LuaValue.POW
                Lua.OP_UNM -> tm = LuaValue.UNM
                Lua.OP_LEN -> tm = LuaValue.LEN
                Lua.OP_LT -> tm = LuaValue.LT
                Lua.OP_LE -> tm = LuaValue.LE
                Lua.OP_CONCAT -> tm = LuaValue.CONCAT
                else -> return null /* else no useful name can be found */
            }
            return net.blueva.luak.lib.DebugLib.NameWhat(tm.tojstring(), "metamethod")
        }

        // return NameWhat if found, null if not
        fun getobjname(p: Prototype, lastpc: Int, reg: Int): NameWhat? {
            var pc = lastpc // currentpc(L, ci);
            var name: LuaString? = p.getlocalname(reg + 1, pc)
            if (name != null)  /* is a local? */
                return net.blueva.luak.lib.DebugLib.NameWhat(name.tojstring(), "local")

            /* else try symbolic execution */
            pc = net.blueva.luak.lib.DebugLib.Companion.findsetreg(p, lastpc, reg)
            if (pc != -1) { /* could find instruction? */
                val i: Int = p.code!![pc]
                when (Lua.GET_OPCODE(i)) {
                    Lua.OP_MOVE -> {
                        val a: Int = Lua.GETARG_A(i)
                        val b: Int = Lua.GETARG_B(i) /* move from `b' to `a' */
                        if (b < a) return net.blueva.luak.lib.DebugLib.Companion.getobjname(
                            p,
                            pc,
                            b
                        ) /* get name for `b' */
                    }

                    Lua.OP_GETTABUP, Lua.OP_GETTABLE -> {
                        val k: Int = Lua.GETARG_C(i) /* key index */
                        val t: Int = Lua.GETARG_B(i) /* table index */
                        val vn: LuaString? = if (Lua.GET_OPCODE(i) === Lua.OP_GETTABLE)
                            p.getlocalname(t + 1, pc)
                        else
                            (if (t < p.upvalues!!.size) p.upvalues!![t]!!.name else net.blueva.luak.lib.DebugLib.Companion.QMARK)
                        val jname: String = net.blueva.luak.lib.DebugLib.Companion.kname(p, pc, k)
                        return net.blueva.luak.lib.DebugLib.NameWhat(
                            jname,
                            if (vn != null && vn.eq_b(ENV)) "global" else "field"
                        )
                    }

                    Lua.OP_GETUPVAL -> {
                        val u: Int = Lua.GETARG_B(i) /* upvalue index */
                        name =
                            if (u < p.upvalues!!.size) p.upvalues!![u]!!.name else net.blueva.luak.lib.DebugLib.Companion.QMARK
                        return if (name == null) null else net.blueva.luak.lib.DebugLib.NameWhat(
                            name.tojstring(),
                            "upvalue"
                        )
                    }

                    Lua.OP_LOADK, Lua.OP_LOADKX -> {
                        val b: Int = if (Lua.GET_OPCODE(i) === Lua.OP_LOADK)
                            Lua.GETARG_Bx(i)
                        else
                            Lua.GETARG_Ax(p.code!![pc + 1])
                        if (p.k!![b]!!.isstring()) {
                            name = p.k!![b]!!.strvalue()
                            return net.blueva.luak.lib.DebugLib.NameWhat(name!!.tojstring(), "constant")
                        }
                    }

                    Lua.OP_SELF -> {
                        val k: Int = Lua.GETARG_C(i) /* key index */
                        val jname: String = net.blueva.luak.lib.DebugLib.Companion.kname(p, pc, k)
                        return net.blueva.luak.lib.DebugLib.NameWhat(jname, "method")
                    }

                    else -> {}
                }
            }
            return null /* no useful name found */
        }

        fun kname(p: Prototype, pc: Int, c: Int): String {
            if (Lua.ISK(c)) {  /* is 'c' a constant? */
                val k: LuaValue = p.k!![Lua.INDEXK(c)]!!
                if (k.isstring()) {  /* literal constant? */
                    return k.tojstring() /* it is its own name */
                } /* else no reasonable name found */
            } else {  /* 'c' is a register */
                val what: NameWhat? = net.blueva.luak.lib.DebugLib.Companion.getobjname(p, pc, c) /* search for 'c' */
                if (what != null && "constant".equals(what.namewhat)) {  /* found a constant name? */
                    return what.name /* 'name' already filled */
                }
                /* else no reasonable name found */
            }
            return "?" /* no reasonable name found */
        }

        /*
	** try to find last instruction before 'lastpc' that modified register 'reg'
	*/
        fun findsetreg(p: Prototype, lastpc: Int, reg: Int): Int {
            var pc: Int
            var setreg = -1 /* keep last instruction that changed 'reg' */
            pc = 0
            while (pc < lastpc) {
                val i: Int = p.code!![pc]
                val op: Int = Lua.GET_OPCODE(i)
                val a: Int = Lua.GETARG_A(i)
                when (op) {
                    Lua.OP_LOADNIL -> {
                        val b: Int = Lua.GETARG_B(i)
                        if (a <= reg && reg <= a + b)  /* set registers from 'a' to 'a+b' */
                            setreg = pc
                    }

                    Lua.OP_TFORCALL -> {
                        if (reg >= a + 2) setreg = pc /* affect all regs above its base */
                    }

                    Lua.OP_CALL, Lua.OP_TAILCALL -> {
                        if (reg >= a) setreg = pc /* affect all registers above base */
                    }

                    Lua.OP_JMP -> {
                        val b: Int = Lua.GETARG_sBx(i)
                        val dest = pc + 1 + b
                        /* jump is forward and do not skip `lastpc'? */
                        if (pc < dest && dest <= lastpc) pc += b /* do the jump */
                    }

                    Lua.OP_TEST -> {
                        if (reg == a) setreg = pc /* jumped code can change 'a' */
                    }

                    Lua.OP_SETLIST -> {
                        // Lua.testAMode(Lua.OP_SETLIST) == false
                        if (((i shr 14) and 0x1ff) == 0) pc++ // if c == 0 then c stored in next op -> skip
                    }

                    else -> if (Lua.testAMode(op) && reg == a)  /* any instruction that set A */
                        setreg = pc
                }
                pc++
            }
            return setreg
        }
    }
}
