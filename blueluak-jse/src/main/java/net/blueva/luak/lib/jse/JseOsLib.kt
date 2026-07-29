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
import net.blueva.luak.Varargs
import net.blueva.luak.lib.OsLib
import java.io.File
import java.io.IOException

/**
 * Subclass of [LibFunction] which implements the standard lua `os` library.
 * 
 * 
 * This contains more complete implementations of the following functions
 * using features that are specific to JSE:
 * 
 *  * `execute()`
 *  * `remove()`
 *  * `rename()`
 *  * `tmpname()`
 * 
 * 
 * 
 * Because the nature of the `os` library is to encapsulate
 * os-specific features, the behavior of these functions varies considerably
 * from their counterparts in the C platform.
 * 
 * 
 * Typically, this library is included as part of a call to
 * [JsePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals(); System.out.println( globals.get("os").get("time").call() ); ` </pre>
 * 
 * 
 * For special cases where the smallest possible footprint is desired,
 * a minimal set of libraries could be loaded
 * directly via [Globals.load] using code such as:
 * <pre> `Globals globals = new Globals(); globals.load(new JseBaseLib()); globals.load(new PackageLib()); globals.load(new JseOsLib()); System.out.println( globals.get("os").get("time").call() ); ` </pre>
 * 
 * However, other libraries such as *MathLib* are not loaded in this case.
 * 
 * 
 * @see LibFunction
 * 
 * @see OsLib
 * 
 * @see JsePlatform
 * 
 * @see net.blueva.luak.lib.jme.JmePlatform
 * 
 * @see [Lua 5.2 OS Lib Reference](http://www.lua.org/manual/5.2/manual.html.6.9)
 */
class JseOsLib
/** public constructor  */
    : OsLib() {
    override fun getenv(varname: String): String? {
        val s = System.getenv(varname)
        return if (s != null) s else System.getProperty(varname)
    }

    override fun execute(command: String?): Varargs {
        var exitValue: Int
        try {
            exitValue = JseProcess(command, null, globals!!.STDOUT, globals!!.STDERR).waitFor()
        } catch (ioe: IOException) {
            exitValue = EXEC_IOEXCEPTION
        } catch (e: InterruptedException) {
            exitValue = EXEC_INTERRUPTED
        } catch (t: Throwable) {
            exitValue = EXEC_ERROR
        }
        if (exitValue == 0) return LuaValue.varargsOf(TRUE, valueOf("exit"), ZERO!!)
        return varargsOf(NIL, valueOf("signal"), valueOf(exitValue))
    }

    @Throws(IOException::class)
    override fun remove(filename: String) {
        val f = File(filename)
        if (!f.exists()) throw IOException("No such file or directory")
        if (!f.delete()) throw IOException("Failed to delete")
    }

    @Throws(IOException::class)
    override fun rename(oldname: String, newname: String) {
        val f = File(oldname)
        if (!f.exists()) throw IOException("No such file or directory")
        if (!f.renameTo(File(newname))) throw IOException("Failed to rename")
    }

    override fun tmpname(): String? {
        try {
            val f = File.createTempFile(TMP_PREFIX, TMP_SUFFIX)
            return f.getAbsolutePath()
        } catch (ioe: IOException) {
            return super.tmpname()
        }
    }

    companion object {
        /** return code indicating the execute() threw an I/O exception  */
        const val EXEC_IOEXCEPTION: Int = 1

        /** return code indicating the execute() was interrupted  */
        val EXEC_INTERRUPTED: Int = -2

        /** return code indicating the execute() threw an unknown exception  */
        val EXEC_ERROR: Int = -3
    }
}
