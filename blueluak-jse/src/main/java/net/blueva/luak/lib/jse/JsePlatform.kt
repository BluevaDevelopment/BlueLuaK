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
package net.blueva.luak.lib.jse

import net.blueva.luak.Globals
import net.blueva.luak.LoadState
import net.blueva.luak.LuaValue
import net.blueva.luak.Varargs
import net.blueva.luak.compiler.LuaC
import net.blueva.luak.lib.Bit32Lib
import net.blueva.luak.lib.CoroutineLib
import net.blueva.luak.lib.DebugLib
import net.blueva.luak.lib.PackageLib
import net.blueva.luak.lib.TableLib

/** The [JsePlatform] class is a convenience class to standardize
 * how globals tables are initialized for the JSE platform.
 * 
 * 
 * It is used to allocate either a set of standard globals using
 * [.standardGlobals] or debug globals using [.debugGlobals]
 * 
 * 
 * A simple example of initializing globals and using them from Java is:
 * <pre> `Globals globals = JsePlatform.standardGlobals(); globals.get("print").call(LuaValue.valueOf("hello, world")); ` </pre>
 * 
 * 
 * Once globals are created, a simple way to load and run a script is:
 * <pre> `globals.load( new FileInputStream("main.lua"), "main.lua" ).call(); ` </pre>
 * 
 * 
 * although `require` could also be used:
 * <pre> `globals.get("require").call(LuaValue.valueOf("main")); ` </pre>
 * For this to succeed, the file "main.lua" must be in the current directory or a resource.
 * See [JseBaseLib] for details on finding scripts using [ResourceFinder].
 * 
 * 
 * The standard globals will contain all standard libraries plus `luajava`:
 * 
 *  * [Globals]
 *  * [JseBaseLib]
 *  * [PackageLib]
 *  * [Bit32Lib]
 *  * [TableLib]
 *  * [StringLib]
 *  * [CoroutineLib]
 *  * [JseMathLib]
 *  * [JseIoLib]
 *  * [JseOsLib]
 *  * [LuajavaLib]
 * 
 * In addition, the [LuaC] compiler is installed so lua files may be loaded in their source form.
 * 
 * 
 * The debug globals are simply the standard globals plus the `debug` library [DebugLib].
 * 
 * 
 * The class ensures that initialization is done in the correct order.
 * 
 * @see Globals
 * 
 * @see net.blueva.luak.lib.jme.JmePlatform
 */
object JsePlatform {
    /**
     * Create a standard set of globals for JSE including all the libraries.
     * 
     * @return Table of globals initialized with the standard JSE libraries
     * @see .debugGlobals
     * @see JsePlatform
     * 
     * @see net.blueva.luak.lib.jme.JmePlatform
     */
    @JvmStatic
    fun standardGlobals(): Globals {
        val globals = Globals()
        globals.load(JseBaseLib())
        globals.load(PackageLib())
        globals.load(Bit32Lib())
        globals.load(TableLib())
        globals.load(JseStringLib())
        globals.load(CoroutineLib())
        globals.load(JseMathLib())
        globals.load(JseIoLib())
        globals.load(JseOsLib())
        globals.load(LuajavaLib())
        LoadState.install(globals)
        LuaC.install(globals)
        return globals
    }

    /** Create standard globals including the [DebugLib] library.
     * 
     * @return Table of globals initialized with the standard JSE and debug libraries
     * @see .standardGlobals
     * @see JsePlatform
     * 
     * @see net.blueva.luak.lib.jme.JmePlatform
     * 
     * @see DebugLib
     */
    @JvmStatic
    fun debugGlobals(): Globals {
        val globals = standardGlobals()
        globals.load(DebugLib())
        return globals
    }


    /** Simple wrapper for invoking a lua function with command line arguments.
     * The supplied function is first given a new Globals object as its environment
     * then the program is run with arguments.
     * @return [Varargs] containing any values returned by mainChunk.
     */
    fun luaMain(mainChunk: LuaValue, args: Array<out String?>): Varargs? {
        val g = standardGlobals()
        val n = args.size
        val vargs = arrayOfNulls<LuaValue>(args.size)
        for (i in 0..<n) vargs[i] = LuaValue.valueOf(args[i])
        val arg: LuaValue = LuaValue.listOf(vargs)
        arg.set("n", n)
        g.set("arg", arg)
        mainChunk.initupvalue1(g)
        return mainChunk.invoke(LuaValue.varargsOf(vargs)!!)
    }
}
