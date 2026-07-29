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

import net.blueva.luak.LuaValue
import net.blueva.luak.lib.BaseLib
import java.io.*

/**
 * Subclass of [BaseLib] and [LibFunction] which implements the lua basic library functions
 * and provides a directory based [ResourceFinder] as the [Globals.finder].
 * 
 * 
 * Since JME has no file system by default, [BaseLib] implements
 * [ResourceFinder] using [Class.getResource].
 * The [JseBaseLib] implements [Globals.finder] by scanning the current directory
 * first, then falling back to   [Class.getResource] if that fails.
 * Otherwise, the behavior is the same as that of [BaseLib].
 * 
 * 
 * Typically, this library is included as part of a call to
 * [JsePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals(); globals.get("print").call(LuaValue.valueOf("hello, world")); ` </pre>
 * 
 * 
 * For special cases where the smallest possible footprint is desired,
 * a minimal set of libraries could be loaded
 * directly via [Globals.load] using code such as:
 * <pre> `Globals globals = new Globals(); globals.load(new JseBaseLib()); globals.get("print").call(LuaValue.valueOf("hello, world")); ` </pre>
 * 
 * However, other libraries such as *PackageLib* are not loaded in this case.
 * 
 * 
 * This is a direct port of the corresponding library in C.
 * @see Globals
 * 
 * @see BaseLib
 * 
 * @see ResourceFinder
 * 
 * @see Globals.finder
 * 
 * @see LibFunction
 * 
 * @see JsePlatform
 * 
 * @see net.blueva.luak.lib.jme.JmePlatform
 * 
 * @see [Lua 5.2 Base Lib Reference](http://www.lua.org/manual/5.2/manual.html.6.1)
 */
class JseBaseLib : BaseLib() {
    /** Perform one-time initialization on the library by creating a table
     * containing the library functions, adding that table to the supplied environment,
     * adding the table to package.loaded, and returning table as the return value.
     * <P>Specifically, extend the library loading to set the default value for [Globals.STDIN]
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env the environment to load into, which must be a Globals instance.
    </P> */
    override fun call(modname: LuaValue?, env: LuaValue): LuaValue {
        super.call(modname, env)
        env.checkglobals()!!.STDIN = System.`in`
        return env
    }


    /**
     * Try to open a file in the current working directory,
     * or fall back to base opener if not found.
     * 
     * This implementation attempts to open the file using new File(filename).
     * It falls back to the base implementation that looks it up as a resource
     * in the class path if not found as a plain file.
     * 
     * @see BaseLib
     * 
     * @see net.blueva.luak.lib.ResourceFinder
     * 
     * 
     * @param filename
     * @return InputStream, or null if not found.
     */
    override fun findResource(filename: String): InputStream? {
        val f = File(filename)
        if (!f.exists()) return super.findResource(filename)
        try {
            return BufferedInputStream(FileInputStream(f))
        } catch (ioe: IOException) {
            return null
        }
    }
}

