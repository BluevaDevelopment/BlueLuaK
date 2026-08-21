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

import net.blueva.luak.lib.BaseLib
import net.blueva.luak.lib.DebugLib
import net.blueva.luak.lib.PackageLib
import net.blueva.luak.lib.ResourceFinder
import net.blueva.luak.io.IOException
import net.blueva.luak.io.InputStream
import net.blueva.luak.io.PrintStream
import net.blueva.luak.io.Reader
import net.blueva.luak.io.standardError
import net.blueva.luak.io.standardOutput

/**
 * Global environment used by BlueLuaK.  Contains global variables referenced by executing lua.
 * 
 * 
 * 
 * <h3>Constructing and Initializing Instances</h3>
 * Typically, this is constructed indirectly by a call to
 * [net.blueva.luak.lib.LuaPlatform.standardGlobals],
 * and then used to load lua scripts for execution as in the following example.
 * <pre> `Globals globals = JvmPlatform.standardGlobals(); globals.load( new StringReader("print 'hello'"), "main.lua" ).call(); ` </pre>
 * The creates a complete global environment with the standard libraries loaded.
 * 
 * 
 * For specialized circumstances, the Globals may be constructed directly and loaded
 * with only those libraries that are needed, for example.
 * <pre> `Globals globals = new Globals(); globals.load( new BaseLib() ); ` </pre>
 * 
 * <h3>Loading and Executing Lua Code</h3>
 * Globals contains convenience functions to load and execute lua source code given a Reader.
 * A simple example is:
 * <pre> `globals.load( new StringReader("print 'hello'"), "main.lua" ).call(); ` </pre>
 * 
 * <h3>Fine-Grained Control of Compiling and Loading Lua</h3>
 * Executable LuaFunctions are created from lua code in several steps
 * 
 *  * find the resource using the platform's [ResourceFinder]
 *  * compile lua to lua bytecode using [Compiler]
 *  * load lua bytecode to a [Prototype] using [Undumper]
 *  * construct [LuaClosure] from [Prototype] with [Globals] using [Loader]
 * 
 * 
 * 
 * There are alternate flows when the direct lua-to-Java bytecode compiling [net.blueva.luak.luajc.LuaJC] is used.
 * 
 *  * compile lua to lua bytecode using [Compiler] or load precompiled code using [Undumper]
 *  * convert lua bytecode to equivalent Java bytecode using [net.blueva.luak.luajc.LuaJC] that implements [Loader] directly
 * 
 * 
 * <h3>Java Field</h3>
 * Certain public fields are provided that contain the current values of important global state:
 * 
 *  * [.STDIN] Current value for standard input in the laaded [IoLib], if any.
 *  * [.STDOUT] Current value for standard output in the loaded [IoLib], if any.
 *  * [.STDERR] Current value for standard error in the loaded [IoLib], if any.
 *  * [.finder] Current loaded [ResourceFinder], if any.
 *  * [.compiler] Current loaded [Compiler], if any.
 *  * [.undumper] Current loaded [Undumper], if any.
 *  * [.loader] Current loaded [Loader], if any.
 * 
 * 
 * <h3>Lua Environment Variables</h3>
 * When using [net.blueva.luak.lib.LuaPlatform] or [net.blueva.luak.lib.jvm.JvmPlatform],
 * these environment variables are created within the Globals.
 * 
 *  * "_G" Pointer to this Globals.
 *  * "_VERSION" String containing the version of Lua implemented by BlueLuaK.
 * 
 * 
 * <h3>Use in Multithreaded Environments</h3>
 * In a multi-threaded server environment, each server thread should create one Globals instance,
 * which will be logically distinct and not interfere with each other, but share certain
 * static immutable resources such as class data and string data.
 * 
 * 
 * 
 * @see net.blueva.luak.lib.jvm.JvmPlatform
 * 
 * @see net.blueva.luak.lib.LuaPlatform
 * 
 * @see LuaValue
 * 
 * @see Compiler
 * 
 * @see Loader
 * 
 * @see Undumper
 * 
 * @see ResourceFinder
 * 
 * @see net.blueva.luak.compiler.LuaC
 * 
 * @see net.blueva.luak.luajc.LuaJC
 */
class Globals : LuaTable() {
    /** The current default input stream.  */
    var STDIN: InputStream? = null

    /** The current default output stream.  */
    var STDOUT: PrintStream? = standardOutput()

    /** The current default error stream.  */
    var STDERR: PrintStream? = standardError()

    /** The installed ResourceFinder for looking files by name.  */
    var finder: ResourceFinder? = null

    /** The currently running thread.  Should not be changed by non-library code.  */
    var running: LuaThread = LuaThread(this)

    /** The BaseLib instance loaded into this Globals  */
    var baselib: BaseLib? = null

    /** The PackageLib instance loaded into this Globals  */
    var package_: PackageLib? = null

    /** The DebugLib instance loaded into this Globals, or null if debugging is not enabled  */
    var debuglib: DebugLib? = null

    /**
     * Objects the host has reclaimed whose `__gc` handler has still to run.
     *
     * Filled by the host, off whatever thread it reclaims on, and emptied here
     * where Lua code can safely be run - which is what [runfinalizers] does.
     */
    internal val finalized: MutableList<LuaValue> = ArrayList()

    /** True once anything at all has been marked for finalization. */
    internal var marksfinalizers: Boolean = false

    /** True while a finalizer runs, so that one cannot set off another. */
    private var finalizing: Boolean = false

    /**
     * Marks [target] to have its `__gc` handler run once it is unreachable.
     *
     * As in Lua this happens when the metatable is set, and only then: a
     * `__gc` added to a metatable that is already in use has no effect on
     * objects that were given it earlier.
     */
    internal fun markforfinalization(target: LuaValue) {
        if (target.gckeeper != null) return
        val keeper: Any? = watchForFinalization(target, finalized)
        if (keeper == null) return // a host that cannot finalize at all
        target.gckeeper = keeper
        marksfinalizers = true
    }

    /**
     * Runs the `__gc` handler of everything the host has reclaimed.
     *
     * Called where the interpreter allocates, which is where Lua runs a step
     * of its own collector, and again whenever `collectgarbage` is asked to
     * collect. A handler that raises is reported as a warning and does not
     * disturb what was running, which is what Lua does with one.
     */
    internal fun runfinalizers() {
        if (!marksfinalizers || finalizing) return
        val due: List<LuaValue> = takeFinalized(finalized)
        if (due.isEmpty()) return
        finalizing = true
        try {
            for (target in due) {
                val handler: LuaValue = target.metatag(LuaValue.GC)
                if (handler.isnil()) continue
                val state: LuaThread.State = running.state
                state.finalizerframepending = true
                try {
                    handler.call(target)
                } catch (failure: LuaError) {
                    baselib?.warning("error in __gc metamethod (" + failure.message + ")")
                } finally {
                    state.finalizerframepending = false
                }
            }
        } finally {
            finalizing = false
        }
    }

    /** Interface for module that converts a Prototype into a LuaFunction with an environment.  */
    interface Loader {
        /** Convert the prototype into a LuaFunction with the supplied environment.  */
        @kotlin.Throws(IOException::class)
        fun load(prototype: Prototype?, chunkname: String?, env: LuaValue?): LuaFunction?
    }

    /** Interface for module that converts lua source text into a prototype.  */
    interface Compiler {
        /** Compile lua source into a Prototype. The InputStream is assumed to be in UTF-8.  */
        @kotlin.Throws(IOException::class)
        fun compile(stream: InputStream?, chunkname: String?): Prototype?
    }

    /** Interface for module that loads lua binary chunk into a prototype.  */
    interface Undumper {
        /** Load the supplied input stream into a prototype.  */
        @kotlin.Throws(IOException::class)
        fun undump(stream: InputStream?, chunkname: String?): Prototype?
    }

    /** Check that this object is a Globals object, and return it, otherwise throw an error.  */
    override fun checkglobals(): Globals {
        return this
    }

    /** The installed loader.
     * @see Loader
     */
    var loader: Loader? = null

    /** The installed compiler.
     * @see Compiler
     */
    var compiler: Compiler? = null

    /** The installed undumper.
     * @see Undumper
     */
    var undumper: Undumper? = null

    /** Convenience function for loading a file that is either binary lua or lua source.
     * @param filename Name of the file to load.
     * @return LuaValue that can be call()'ed or invoke()'ed.
     * @throws LuaError if the file could not be loaded.
     */
    fun loadfile(filename: String?): LuaValue? {
        try {
            val stream = finder?.findResource(filename) ?: throw LuaError("load $filename: no resource")
            return load(stream, "@" + filename, "bt", this)
        } catch (l: LuaError) {
            // Already says what is wrong in Lua's own words - where in the
            // file, and what about it - so nothing is added to it here.
            throw l
        } catch (e: Exception) {
            return error("load " + filename + ": " + e)
        }
    }

    /** Convenience function to load a string value as a script.  Must be lua source.
     * @param script Contents of a lua script, such as "print 'hello, world.'"
     * @param chunkname Name that will be used within the chunk as the source.
     * @return LuaValue that may be executed via .call(), .invoke(), or .method() calls.
     * @throws LuaError if the script could not be compiled.
     */
    fun load(script: String, chunkname: String?): LuaValue? {
        return load(net.blueva.luak.Globals.StrReader(script), chunkname)
    }

    /** Convenience function to load a string value as a script.  Must be lua source.
     * @param script Contents of a lua script, such as "print 'hello, world.'"
     * @return LuaValue that may be executed via .call(), .invoke(), or .method() calls.
     * @throws LuaError if the script could not be compiled.
     */
    fun load(script: String): LuaValue? {
        return load(net.blueva.luak.Globals.StrReader(script), script)
    }

    /** Convenience function to load a string value as a script with a custom environment.
     * Must be lua source.
     * @param script Contents of a lua script, such as "print 'hello, world.'"
     * @param chunkname Name that will be used within the chunk as the source.
     * @param environment LuaTable to be used as the environment for the loaded function.
     * @return LuaValue that may be executed via .call(), .invoke(), or .method() calls.
     * @throws LuaError if the script could not be compiled.
     */
    fun load(script: String, chunkname: String?, environment: LuaTable?): LuaValue? {
        return load(net.blueva.luak.Globals.StrReader(script), chunkname, environment)
    }

    /** Load the content form a reader as a text file.  Must be lua source.
     * The source is converted to UTF-8, so any characters appearing in quoted literals
     * above the range 128 will be converted into multiple bytes.
     * @param reader Reader containing text of a lua script, such as "print 'hello, world.'"
     * @param chunkname Name that will be used within the chunk as the source.
     * @return LuaValue that may be executed via .call(), .invoke(), or .method() calls.
     * @throws LuaError if the script could not be compiled.
     */
    fun load(reader: Reader, chunkname: String?): LuaValue? {
        return load(net.blueva.luak.Globals.UTF8Stream(reader), chunkname, "t", this)
    }

    /** Load the content form a reader as a text file, supplying a custom environment.
     * Must be lua source. The source is converted to UTF-8, so any characters
     * appearing in quoted literals above the range 128 will be converted into
     * multiple bytes.
     * @param reader Reader containing text of a lua script, such as "print 'hello, world.'"
     * @param chunkname Name that will be used within the chunk as the source.
     * @param environment LuaTable to be used as the environment for the loaded function.
     * @return LuaValue that may be executed via .call(), .invoke(), or .method() calls.
     * @throws LuaError if the script could not be compiled.
     */
    fun load(reader: Reader, chunkname: String?, environment: LuaTable?): LuaValue? {
        return load(net.blueva.luak.Globals.UTF8Stream(reader), chunkname, "t", environment)
    }

    /** Load the content form an input stream as a binary chunk or text file.
     * @param is InputStream containing a lua script or compiled lua"
     * @param chunkname Name that will be used within the chunk as the source.
     * @param mode String containing 'b' or 't' or both to control loading as binary or text or either.
     * @param environment LuaTable to be used as the environment for the loaded function.
     */
    fun load(`is`: InputStream, chunkname: String?, mode: String, environment: LuaValue?): LuaValue? {
        try {
            val p: Prototype? = loadPrototype(`is`, chunkname, mode)
            return loader!!.load(p, chunkname, environment)
        } catch (l: LuaError) {
            throw l
        } catch (e: Exception) {
            throw LuaError("load " + chunkname + ": " + e, e)
        }
    }

    /** Load lua source or lua binary from an input stream into a Prototype.
     * The InputStream is either a binary lua chunk starting with the lua binary chunk signature,
     * or a text input file.  If it is a text input file, it is interpreted as a UTF-8 byte sequence.
     * @param is Input stream containing a lua script or compiled lua"
     * @param chunkname Name that will be used within the chunk as the source.
     * @param mode String containing 'b' or 't' or both to control loading as binary or text or either.
     */
    @kotlin.Throws(IOException::class)
    fun loadPrototype(`is`: InputStream, chunkname: String?, mode: String): Prototype? {
        var `is`: InputStream = `is`
        if (!`is`.markSupported()) `is` = net.blueva.luak.Globals.BufferedStream(`is`)
        `is`.mark(4)
        val first: Int = `is`.read()
        `is`.reset()
        // The signature byte says which kind of chunk is really there, so the
        // mode is checked against that rather than against what the caller
        // hoped for: asking for text and handing over a dump is refused, not
        // parsed as source.
        if (first == LoadState.LUA_SIGNATURE[0].toInt()) {
            if (mode.indexOf('b') < 0) {
                error("attempt to load a binary chunk (mode is '" + mode + "')")
            }
            if (undumper == null) error("No undumper.")
            return undumper!!.undump(`is`, chunkname)
        }
        if (mode.indexOf('t') < 0) {
            error("attempt to load a text chunk (mode is '" + mode + "')")
        }
        return compilePrototype(`is`, chunkname)
    }

    /** Compile lua source from a Reader into a Prototype. The characters in the reader
     * are converted to bytes using the UTF-8 encoding, so a string literal containing
     * characters with codepoints 128 or above will be converted into multiple bytes.
     */
    @kotlin.Throws(IOException::class)
    fun compilePrototype(reader: Reader, chunkname: String?): Prototype? {
        return compilePrototype(net.blueva.luak.Globals.UTF8Stream(reader), chunkname)
    }

    /** Compile lua source from an InputStream into a Prototype.
     * The input is assumed to be UTf-8, but since bytes in the range 128-255 are passed along as
     * literal bytes, any ASCII-compatible encoding such as ISO 8859-1 may also be used.
     */
    @kotlin.Throws(IOException::class)
    fun compilePrototype(stream: InputStream?, chunkname: String?): Prototype? {
        if (compiler == null) error("No compiler.")
        return compiler!!.compile(stream, chunkname)
    }

    /** Function which yields the current thread.
     *
     * Only usable from within the interpreter's suspend-aware call chain (see
     * [LuaValue.callSuspend]); calling this directly from regular, non-suspend
     * Kotlin code has nowhere to suspend to and always fails, matching real
     * Lua's C-call boundary restriction. Use [yieldSuspend] instead when
     * writing a library function that should support being yielded through.
     * @param args  Arguments to supply as return values in the resume function of the resuming thread.
     * @return Values supplied as arguments to the resume() call that reactivates this thread.
     */
    fun yield(args: Varargs?): Varargs {
        if (running.isMainThread) throw LuaError("attempt to yield from outside a coroutine")
        return runLuaSync { yieldSuspend(args) }
    }

    /** Suspending counterpart to [yield]; see its doc for details. */
    suspend fun yieldSuspend(args: Varargs?): Varargs {
        if (running.isMainThread) throw LuaError("attempt to yield from outside a coroutine")
        val s: LuaThread.State = running.state
        return s.lua_yield(args)
    }

    /** Reader implementation to read chars from a String in JME or JVM.  */
    internal class StrReader(val s: String) : Reader() {
        var i: Int = 0
        val n: Int

        init {
            n = s.length
        }

        @kotlin.Throws(IOException::class)
        override fun close() {
            i = n
        }

        @kotlin.Throws(IOException::class)
        override fun read(): Int {
            return if (i < n) s[i++].code else -1
        }

        @kotlin.Throws(IOException::class)
        override fun read(cbuf: CharArray, off: Int, len: Int): Int {
            var j = 0
            while (j < len && i < n) {
                cbuf[off + j] = s[i]
                ++j
                ++i
            }
            return if (j > 0 || len == 0) j else -1
        }
    }

    /* Abstract base class to provide basic buffered input storage and delivery.
	 * This class may be moved to its own package in the future.
	 */
    abstract class AbstractBufferedStream protected constructor(buflen: Int) : InputStream() {
        protected var b: ByteArray
        protected var i: Int = 0
        protected var j: Int = 0

        init {
            this.b = ByteArray(buflen)
        }

        @kotlin.Throws(IOException::class)
        protected abstract fun avail(): Int

        @kotlin.Throws(IOException::class)
        override fun read(): Int {
            val a = avail()
            return (if (a <= 0) -1 else 0xff and b[i++].toInt())
        }

        @kotlin.Throws(IOException::class)
        override fun read(b: ByteArray): Int {
            return read(b, 0, b.size)
        }

        @kotlin.Throws(IOException::class)
        override fun read(b: ByteArray, i0: Int, n: Int): Int {
            val a = avail()
            if (a <= 0) return -1
            val n_read: Int = minOf(a, n)
            arrayCopy(this.b, i, b, i0, n_read)
            i += n_read
            return n_read
        }

        @kotlin.Throws(IOException::class)
        override fun skip(n: Long): Long {
            val k: Long = minOf(n, (j - i).toLong())
            i += k.toInt()
            return k
        }

        @kotlin.Throws(IOException::class)
        override fun available(): Int {
            return j - i
        }
    }

    /**  Simple converter from Reader to InputStream using UTF8 encoding that will work
     * on both JME and JVM.
     * This class may be moved to its own package in the future.
     */
    internal class UTF8Stream(r: Reader) : AbstractBufferedStream(96) {
        private val c = CharArray(32)
        private val r: Reader

        init {
            this.r = r
        }

        @kotlin.Throws(IOException::class)
        override fun avail(): Int {
            if (i < j) return j - i
            var n: Int = r.read(c, 0, c.size)
            if (n < 0) return -1
            if (n == 0) {
                val u: Int = r.read()
                if (u < 0) return -1
                c[0] = u.toChar()
                n = 1
            }
            j = LuaString.encodeToUtf8(c, n, b, 0.also { i = it })
            return j
        }

        @kotlin.Throws(IOException::class)
        override fun close() {
            r.close()
        }
    }

    /** Simple buffered InputStream that supports mark.
     * Used to examine an InputStream for a 4-byte binary lua signature,
     * and fall back to text input when the signature is not found,
     * as well as speed up normal compilation and reading of lua scripts.
     * This class may be moved to its own package in the future.
     */
    class BufferedStream(buflen: Int, s: InputStream) : AbstractBufferedStream(buflen) {
        private val s: InputStream

        constructor(s: InputStream) : this(128, s)

        init {
            this.s = s
        }

        @kotlin.Throws(IOException::class)
        override fun avail(): Int {
            if (i < j) return j - i
            if (j >= b.size) {
                j = 0
                i = j
            }
            // leave previous bytes in place to implement mark()/reset().
            var n: Int = s.read(b, j, b.size - j)
            if (n < 0) return -1
            if (n == 0) {
                val u: Int = s.read()
                if (u < 0) return -1
                b[j] = u.toByte()
                n = 1
            }
            j += n
            return n
        }

        @kotlin.Throws(IOException::class)
        override fun close() {
            s.close()
        }

                override fun mark(n: Int) {
            if (i > 0 || n > b.size) {
                val dest = if (n > b.size) ByteArray(n) else b
                arrayCopy(b, i, dest, 0, j - i)
                j -= i
                i = 0
                b = dest
            }
        }

        override fun markSupported(): Boolean {
            return true
        }

                @kotlin.Throws(IOException::class)
        override fun reset() {
            i = 0
        }
    }
}
