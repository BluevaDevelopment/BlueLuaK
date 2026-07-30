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

import net.blueva.luak.lib.jse.JsePlatform
import net.blueva.luak.luajc.LuaJC
import java.io.*
import java.util.*

/**
 * lua command for use in JSE environments.
 */
object lua {
    private val version = Lua._VERSION + " Copyright (c) 2012 Luaj.org.org"

    private val usage = "usage: java -cp luaj-jse.jar lua [options] [script [args]].\n" +
            "Available options are:\n" +
            "  -e stat  execute string 'stat'\n" +
            "  -l name  require library 'name'\n" +
            "  -i       enter interactive mode after executing 'script'\n" +
            "  -v       show version information\n" +
            "  -b      	use luajc bytecode-to-bytecode compiler (requires bcel on class path)\n" +
            "  -n      	nodebug - do not load debug library by default\n" +
            "  -p      	print the prototype\n" +
            "  -c enc  	use the supplied encoding 'enc' for input files\n" +
            "  --       stop handling options\n" +
            "  -        execute stdin and stop handling options"

    private fun usageExit() {
        println(usage)
        System.exit(-1)
    }

    private var globals: Globals? = null
    private var print = false
    private var encoding: String? = null

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        // process args

        var interactive = (args.size == 0)
        var versioninfo = false
        var processing = true
        var nodebug = false
        var luajc = false
        var libs: Vector<String>? = null
        try {
            // stateful argument processing
            run {
                var i = 0
                while (i < args.size) {
                    if (!processing || !args[i].startsWith("-")) {
                        // input file - defer to last stage
                        break
                    } else if (args[i].length <= 1) {
                        // input file - defer to last stage
                        break
                    } else {
                        when (args[i].get(1)) {
                            'e' -> if (++i >= args.size) lua.usageExit()
                            'b' -> luajc = true
                            'l' -> {
                                if (++i >= args.size) lua.usageExit()
                                libs = libs ?: Vector<String>()
                                libs.addElement(args[i])
                            }

                            'i' -> interactive = true
                            'v' -> versioninfo = true
                            'n' -> nodebug = true
                            'p' -> lua.print = true
                            'c' -> {
                                if (++i >= args.size) lua.usageExit()
                                lua.encoding = args[i]
                            }

                            '-' -> {
                                if (args[i].length > 2) lua.usageExit()
                                processing = false
                            }

                            else -> lua.usageExit()
                        }
                    }
                    i++
                }
            }

            // echo version
            if (versioninfo) println(version)


            // new lua state
            globals = if (nodebug) JsePlatform.standardGlobals() else JsePlatform.debugGlobals()
            if (luajc) LuaJC.install(globals!!)
            run {
                var i = 0
                val n = if (libs != null) libs.size else 0
                while (i < n) {
                    lua.loadLibrary(libs!!.elementAt(i) as String?)
                    i++
                }
            }


            // input script processing
            processing = true
            var i = 0
            while (i < args.size) {
                if (!processing || !args[i].startsWith("-")) {
                    lua.processScript(FileInputStream(args[i]), args[i], args, i)
                    break
                } else if ("-" == args[i]) {
                    lua.processScript(System.`in`, "=stdin", args, i)
                    break
                } else {
                    when (args[i].get(1)) {
                        'l', 'c' -> ++i
                        'e' -> {
                            ++i
                            lua.processScript(ByteArrayInputStream(args[i].toByteArray()), "string", args, i)
                        }

                        '-' -> processing = false
                    }
                }
                i++
            }

            if (interactive) interactiveMode()
        } catch (ioe: IOException) {
            System.err.println(ioe.toString())
            System.exit(-2)
        }
    }

    @Throws(IOException::class)
    private fun loadLibrary(libname: String?) {
        val slibname: LuaValue = LuaValue.valueOf(libname)
        try {
            // load via plain require
            globals!!.get("require")!!.call(slibname)
        } catch (e: Exception) {
            try {
                // load as java class
                val v = Class.forName(libname).newInstance() as LuaValue
                v.call(slibname, globals)
            } catch (f: Exception) {
                throw IOException("loadLibrary(" + libname + ") failed: " + e + "," + f)
            }
        }
    }

    @Throws(IOException::class)
    private fun processScript(script: InputStream, chunkname: String?, args: Array<String>?, firstarg: Int) {
        var script = script
        try {
            var c: LuaValue
            try {
                script = BufferedInputStream(script)
                c = (if (encoding != null) globals!!.load(
                    InputStreamReader(script, encoding),
                    chunkname
                ) else globals!!.load(script, chunkname, "bt", globals))!!
            } finally {
                script.close()
            }
            if (print && c.isclosure()) Print.print(c.checkclosure()!!.p)
            val scriptargs = setGlobalArg(chunkname, args, firstarg, globals!!)
            c.invoke(scriptargs!!)
        } catch (e: Exception) {
            e.printStackTrace(System.err)
        }
    }

    private fun setGlobalArg(chunkname: String?, args: Array<String>?, i: Int, globals: LuaValue): Varargs? {
        if (args == null) return LuaValue.NONE
        val arg = LuaValue.tableOf()
        for (j in args.indices) arg.set(j - i, LuaValue.valueOf(args[j]))
        arg.set(0, LuaValue.valueOf(chunkname))
        arg.set(-1, LuaValue.valueOf("luaj"))
        globals.set("arg", arg)
        return arg.unpack()
    }

    @Throws(IOException::class)
    private fun interactiveMode() {
        val reader = BufferedReader(InputStreamReader(System.`in`))
        while (true) {
            print("> ")
            System.out.flush()
            val line = reader.readLine()
            if (line == null) return
            processScript(ByteArrayInputStream(line.toByteArray()), "=stdin", null, 0)
        }
    }
}
